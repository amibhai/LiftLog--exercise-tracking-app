package io.github.amibhai.liftlog.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.RoutineSummary
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.time.daysAgo
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val exerciseCount: Int? = null,
    /** Starting another workout has to wait while this one is in progress. */
    val activeWorkoutId: WorkoutId? = null,
    val repeatLast: RepeatLast? = null,
    /** Null until loaded. */
    val routines: List<RoutineCardUiModel>? = null,
    /** A workout that was just started, for the screen to open. */
    val openWorkout: WorkoutId? = null,
    /** A routine that was just created, for its editor to open. */
    val openRoutine: RoutineId? = null,
    /** A routine that was just deleted and can still be brought back. */
    val deletedRoutine: RoutineId? = null,
)

/** "Repeat last Push" (SPEC §5): the routine of the last workout, or that workout when it had none. */
@Immutable
data class RepeatLast(val workoutId: WorkoutId, val routineId: RoutineId?, val routineName: String?)

@Immutable
data class RoutineCardUiModel(
    val id: RoutineId,
    val name: String,
    val exercises: List<String>,
    /** Null when it was never done. */
    val lastDoneDaysAgo: Int?,
)

internal data class HomeLocalState(
    val openWorkout: WorkoutId? = null,
    val openRoutine: RoutineId? = null,
    val deletedRoutine: RoutineId? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    exercises: ExerciseRepository,
    workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val startWorkout: StartWorkout,
    private val ids: IdGenerator,
    private val work: SequentialWork,
    private val clock: Clock,
) : ViewModel() {

    private val local = MutableStateFlow(HomeLocalState())

    val uiState: StateFlow<HomeUiState> = combine(
        exercises.observeStats(),
        workouts.observeActive(),
        workouts.observeFinished(limit = 1).map { it.firstOrNull() },
        routines.observeAll(),
        local,
    ) { stats, active, last, routines, local ->
        HomeUiState(
            exerciseCount = stats.totalExercises,
            activeWorkoutId = active?.id,
            repeatLast = last?.let { repeatLast(it, routines) },
            routines = routines.map(::card),
            openWorkout = local.openWorkout,
            openRoutine = local.openRoutine,
            deletedRoutine = local.deletedRoutine,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun startEmpty() = start { startWorkout.empty() }

    fun startRoutine(id: RoutineId) = start { startWorkout.fromRoutine(id) }

    fun repeatLast() {
        val repeat = uiState.value.repeatLast ?: return
        start { repeat.routineId?.let { startWorkout.fromRoutine(it) } ?: startWorkout.repeat(repeat.workoutId) }
    }

    fun onWorkoutOpened() = local.update { it.copy(openWorkout = null) }

    /** Creates an empty routine and then opens it; its editor deletes it again if it stays empty. */
    fun newRoutine() {
        val routine = Routine(RoutineId(ids.newId()), name = "")
        work.submit {
            routines.create(routine)
            local.update { it.copy(openRoutine = routine.id) }
        }
    }

    fun onRoutineOpened() = local.update { it.copy(openRoutine = null) }

    fun deleteRoutine(id: RoutineId) {
        work.submit { routines.delete(id) }
        local.update { it.copy(deletedRoutine = id) }
    }

    fun undoDeleteRoutine() {
        val id = local.value.deletedRoutine ?: return
        work.submit { routines.restore(id) }
        local.update { it.copy(deletedRoutine = null) }
    }

    fun onDeleteMessageShown() = local.update { it.copy(deletedRoutine = null) }

    private fun start(create: suspend () -> WorkoutId?) {
        viewModelScope.launch {
            val id = create() ?: return@launch
            local.update { it.copy(openWorkout = id) }
        }
    }

    private fun repeatLast(last: Workout, routines: List<RoutineSummary>): RepeatLast {
        val routine = routines.firstOrNull { it.routine.id == last.routineId }?.routine
        return RepeatLast(last.id, routine?.id, routine?.name?.ifBlank { null })
    }

    private fun card(summary: RoutineSummary) = RoutineCardUiModel(
        id = summary.routine.id,
        name = summary.routine.name,
        exercises = summary.routine.exercises.map { it.exercise.name },
        lastDoneDaysAgo = summary.lastPerformedAt?.let { daysAgo(it, clock.millis(), ZoneId.systemDefault()) },
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
