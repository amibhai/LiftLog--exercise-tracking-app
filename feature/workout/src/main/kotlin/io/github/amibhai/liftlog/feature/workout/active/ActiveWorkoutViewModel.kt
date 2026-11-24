package io.github.amibhai.liftlog.feature.workout.active

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.time.partOfDay
import io.github.amibhai.liftlog.core.domain.workout.totals
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

/** The workout in progress, for the banner that leads back to it. */
@Immutable
data class ActiveWorkoutUiState(
    val id: WorkoutId,
    val title: WorkoutTitle,
    val startedAt: Long,
    val completedSets: Int,
    /** When the current rest ends; null while not resting. */
    val restEndsAt: Long?,
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(workouts: WorkoutRepository) : ViewModel() {
    val uiState: StateFlow<ActiveWorkoutUiState?> = workouts.observeActive()
        .map { it?.let(::banner) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private fun banner(workout: Workout) = ActiveWorkoutUiState(
        id = workout.id,
        title = WorkoutTitle(workout.name, partOfDay(workout.startedAt, ZoneId.systemDefault())),
        startedAt = workout.startedAt,
        completedSets = workout.totals().completedSets,
        restEndsAt = workout.restTimer?.endsAt,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
