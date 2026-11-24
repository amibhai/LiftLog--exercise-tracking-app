package io.github.amibhai.liftlog.feature.routines.programs.template

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.program.ProgramTemplates
import io.github.amibhai.liftlog.core.domain.program.TemplateDayName
import io.github.amibhai.liftlog.core.domain.program.TemplateKey
import io.github.amibhai.liftlog.core.domain.progress.sessionProgress
import io.github.amibhai.liftlog.core.domain.progression.Loading
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.usecase.CreateProgram
import io.github.amibhai.liftlog.feature.routines.navigation.TemplateDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed interface TemplateUiState {
    data object Loading : TemplateUiState

    @Immutable
    data class Ready(
        val key: TemplateKey,
        val days: List<TemplateDayUiModel>,
        /** The lifts that need a training max. */
        val lifts: List<LiftUiModel>,
        val unit: WeightUnit,
        val isCreating: Boolean = false,
        /** Set once the program exists: its page opens. */
        val created: ProgramId? = null,
    ) : TemplateUiState
}

@Immutable
data class TemplateDayUiModel(val name: TemplateDayName, val exercises: List<TemplateExerciseUiModel>)

@Immutable
data class TemplateExerciseUiModel(val name: String, val sets: Int, val reps: IntRange?)

/** [suggestion] comes from the history, when there is one. */
@Immutable
data class LiftUiModel(val id: ExerciseId, val name: String, val suggestion: Weight?)

/** What a built-in program contains, and what it needs to know before it starts (SPEC §7). */
@HiltViewModel
class TemplateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exercises: ExerciseRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val createProgram: CreateProgram,
    private val clock: Clock,
) : ViewModel() {

    private val template = ProgramTemplates.of(
        TemplateKey.valueOf(savedStateHandle.toRoute<TemplateDestination>().key),
    )
    private val state = MutableStateFlow<TemplateUiState>(TemplateUiState.Loading)
    val uiState: StateFlow<TemplateUiState> = state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    /** [dayNames] and [name] are in the language of the user. */
    fun create(name: String, dayNames: Map<TemplateDayName, String>, trainingMaxes: Map<ExerciseId, Weight>) {
        val ready = state.value as? TemplateUiState.Ready
        if (ready == null || ready.isCreating) return
        state.value = ready.copy(isCreating = true)
        viewModelScope.launch {
            val id = createProgram(template, name, dayNames, trainingMaxes)
            state.update { (it as TemplateUiState.Ready).copy(created = id) }
        }
    }

    private suspend fun load() {
        val current = settings.settings.first()
        val infos = exercises.infos(template.exercises.toList()).associateBy { it.id }
        state.value = TemplateUiState.Ready(
            key = template.key,
            days = template.days.map { day ->
                TemplateDayUiModel(
                    name = day.name,
                    exercises = day.slots.mapNotNull { slot ->
                        infos[slot.exercise]?.let { TemplateExerciseUiModel(it.name, slot.sets, slot.reps) }
                    },
                )
            },
            lifts = lifts(infos, current),
            unit = current.weightUnit,
        )
    }

    private suspend fun lifts(infos: Map<ExerciseId, ExerciseInfo>, settings: UserSettings): List<LiftUiModel> {
        val now = clock.millis()
        val sessions = progress.sessionsBefore(now, template.trainingMaxLifts.toSet())
        return template.trainingMaxLifts.mapNotNull(infos::get).map { lift ->
            val recent = sessions[lift.id].orEmpty().filter { it.at >= now - RecentMillis }
            val best = sessionProgress(lift.type, recent, settings.e1rmFormula).mapNotNull { it.bestE1rm }.maxOrNull()
            val loading = Loading.of(lift, settings)
            LiftUiModel(lift.id, lift.name, best?.let { loading.part(Weight(it), TRAINING_MAX_PERCENT) })
        }
    }

    private companion object {
        /** A training max is usually 90% of the one-rep max. */
        const val TRAINING_MAX_PERCENT = 90

        /** Older sessions say little about today. */
        val RecentMillis = TimeUnit.DAYS.toMillis(180)
    }
}
