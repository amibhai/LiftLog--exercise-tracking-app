package io.github.amibhai.liftlog.feature.settings.importer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.concurrent.AppDispatchers
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.importer.ImportedLog
import io.github.amibhai.liftlog.core.domain.importer.LogSource
import io.github.amibhai.liftlog.core.domain.importer.parseLog
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.repository.BackupRepository
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.search.SearchIndex
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.usecase.ImportChoice
import io.github.amibhai.liftlog.core.domain.usecase.ImportLog
import io.github.amibhai.liftlog.core.domain.usecase.ImportResult
import io.github.amibhai.liftlog.core.domain.usecase.NameMatch
import io.github.amibhai.liftlog.core.domain.usecase.SearchExercises
import io.github.amibhai.liftlog.feature.settings.navigation.ImportDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface ImportUiState {
    data object Reading : ImportUiState

    /** The file cannot be read, or is no export of a known app. */
    data object Unknown : ImportUiState

    @Immutable
    data class Ready(
        val source: LogSource,
        val workouts: Int,
        val first: LocalDate,
        val last: LocalDate,
        val unreadableRows: Int,
        /** What the weights of the file are in. */
        val unit: WeightUnit,
        /** The file does not say its unit, so it has to be checked. */
        val isUnitGuessed: Boolean,
        /** Names to check come first. */
        val rows: List<ImportRow>,
        val isImporting: Boolean = false,
        /** Results of the search for another exercise. */
        val found: List<CatalogExercise> = emptyList(),
    ) : ImportUiState

    @Immutable
    data class Done(val result: ImportResult) : ImportUiState
}

/** A name in the file and what becomes of it. */
@Immutable
data class ImportRow(
    val name: String,
    val choice: ImportChoice,
    /** Of the chosen exercise. */
    val exerciseName: String?,
    val needsCheck: Boolean,
)

/** Checks and imports the export of another app (SPEC §9). */
@HiltViewModel
class ImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val backups: BackupRepository,
    private val importLog: ImportLog,
    private val exercises: ExerciseRepository,
    private val searchExercises: SearchExercises,
    private val settings: SettingsRepository,
    private val work: SequentialWork,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val uri = savedStateHandle.toRoute<ImportDestination>().uri
    private val state = MutableStateFlow<ImportUiState>(ImportUiState.Reading)
    val uiState: StateFlow<ImportUiState> = state.asStateFlow()

    private var log: ImportedLog? = null
    private var index: SearchIndex? = null
    private var search: Job? = null

    init {
        viewModelScope.launch { read() }
    }

    fun setUnit(unit: WeightUnit) = ready { it.copy(unit = unit) }

    fun choose(name: String, exercise: CatalogExercise) =
        change(name) { it.copy(choice = ImportChoice.Existing(exercise.id), exerciseName = exercise.name) }

    fun create(name: String) = change(name) { it.copy(choice = ImportChoice.Create, exerciseName = null) }

    fun skip(name: String) = change(name) { it.copy(choice = ImportChoice.Skip, exerciseName = null) }

    fun search(query: String) {
        search?.cancel()
        search = viewModelScope.launch {
            val found = withContext(dispatchers.default) {
                val prepared = index ?: SearchIndex(exercises.observeCatalog().first()).also { index = it }
                searchExercises(query, prepared, emptyMap()).take(MOST_FOUND)
            }
            ready { it.copy(found = found) }
        }
    }

    fun import() {
        val ready = state.value as? ImportUiState.Ready
        val log = log
        if (ready == null || log == null || ready.isImporting) return
        state.value = ready.copy(isImporting = true)
        // In the queue of the app, so that it goes on when the screen is left.
        work.submit {
            val choices = ready.rows.associate { it.name to it.choice }
            val result = importLog.import(log, choices, ready.unit, ZoneId.systemDefault())
            state.value = ImportUiState.Done(result)
        }
    }

    private suspend fun read() {
        val text = backups.readText(uri)
        val parsed = withContext(dispatchers.default) { text?.let(::parseLog) }?.takeIf { it.workouts.isNotEmpty() }
        log = parsed
        state.value = if (parsed == null) ImportUiState.Unknown else ready(parsed)
    }

    private suspend fun ready(log: ImportedLog): ImportUiState.Ready {
        val matches = withContext(dispatchers.default) { importLog.matches(log) }
        return ImportUiState.Ready(
            source = log.source,
            workouts = log.workouts.size,
            first = log.workouts.first().startedAt.toLocalDate(),
            last = log.workouts.last().startedAt.toLocalDate(),
            unreadableRows = log.unreadableRows,
            unit = log.unit ?: settings.settings.first().weightUnit,
            isUnitGuessed = log.unit == null,
            rows = matches.map { it.toRow() }.sortedByDescending { it.needsCheck },
        )
    }

    private fun NameMatch.toRow() = ImportRow(
        name = name,
        choice = exercise?.let { ImportChoice.Existing(it.id) } ?: ImportChoice.Create,
        exerciseName = exercise?.name,
        needsCheck = !isSure,
    )

    private fun change(name: String, change: (ImportRow) -> ImportRow) = ready { ready ->
        ready.copy(rows = ready.rows.map { if (it.name == name) change(it).copy(needsCheck = false) else it })
    }

    private fun ready(change: (ImportUiState.Ready) -> ImportUiState.Ready) =
        state.update { if (it is ImportUiState.Ready) change(it) else it }

    private companion object {
        const val MOST_FOUND = 30
    }
}
