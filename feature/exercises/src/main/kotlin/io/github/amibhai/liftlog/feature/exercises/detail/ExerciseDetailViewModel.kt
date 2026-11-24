package io.github.amibhai.liftlog.feature.exercises.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.time.daysAgo
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.feature.exercises.navigation.ExerciseDetailDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState

    data object NotFound : ExerciseDetailUiState

    @Immutable
    data class Loaded(
        val exercise: ExerciseDetail,
        val sessions: List<SessionUiModel> = emptyList(),
        val settings: UserSettings = UserSettings(),
    ) : ExerciseDetailUiState
}

/** One past workout with the exercise. */
@Immutable
data class SessionUiModel(val workoutId: WorkoutId, val daysAgo: Int, val sets: List<String>)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exercises: ExerciseRepository,
    history: HistoryRepository,
    settings: SettingsRepository,
    private val editor: ExerciseEditor,
    private val clock: Clock,
) : ViewModel() {

    private val id = ExerciseId(savedStateHandle.toRoute<ExerciseDetailDestination>().exerciseId)

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        exercises.observeDetail(id),
        history.observeSessions(id, HISTORY_LIMIT),
        settings.settings,
    ) { detail, sessions, current ->
        if (detail == null) {
            ExerciseDetailUiState.NotFound
        } else {
            val text = SetText(current.weightUnit)
            ExerciseDetailUiState.Loaded(detail, sessions.map { it.toUiModel(detail, text) }, current)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExerciseDetailUiState.Loading)

    fun setFavorite(favorite: Boolean) = edit { setFavorite(id, favorite) }

    fun setHidden(hidden: Boolean) = edit { setHidden(id, hidden) }

    fun setNote(note: String) = edit { setStickyNote(id, note) }

    fun setDefaults(defaults: ExerciseDefaults) = edit { setDefaults(id, defaults) }

    fun delete() = edit { deleteCustom(id) }

    private fun edit(change: suspend ExerciseEditor.() -> Unit) {
        viewModelScope.launch { editor.change() }
    }

    private fun ExerciseSession.toUiModel(detail: ExerciseDetail, text: SetText) = SessionUiModel(
        workoutId = workoutId,
        daysAgo = daysAgo(at, clock.millis(), ZoneId.systemDefault()),
        sets = sets.map { text.summary(detail.type, it.values) },
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val HISTORY_LIMIT = 20
    }
}
