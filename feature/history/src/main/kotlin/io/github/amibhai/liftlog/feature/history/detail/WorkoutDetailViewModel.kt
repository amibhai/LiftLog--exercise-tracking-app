package io.github.amibhai.liftlog.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import io.github.amibhai.liftlog.core.domain.workout.routineFrom
import io.github.amibhai.liftlog.core.domain.workout.updatedFrom
import io.github.amibhai.liftlog.feature.history.navigation.WorkoutDetailDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** One finished workout: its sets, and what can be done with it (SPEC §5, §14). */
@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val startWorkout: StartWorkout,
    private val progress: ProgressRepository,
    private val ids: IdGenerator,
    private val work: SequentialWork,
    settings: SettingsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<WorkoutDetailDestination>()
    private val id = WorkoutId(route.workoutId)
    private val local = MutableStateFlow(DetailLocalState())

    private class Stored(val workout: Workout?)

    private val stored: StateFlow<Stored?> = workouts.observe(id).map(::Stored)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val routine = stored.map { it?.workout?.routineId }
        .distinctUntilChanged()
        .flatMapLatest { routineId -> routineId?.let(routines::observe) ?: flowOf(null) }

    val uiState: StateFlow<WorkoutDetailUiState> = combine(
        combine(stored, routine, workouts.observeActive().map { it?.id }, settings.settings, local, ::DetailInputs),
        combine(progress.observeRecordsOf(id), progress.observeMuscleWork(id), ::DetailExtras),
        ::state,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutDetailUiState.Loading)

    /** Starts the same exercises again, filled in from this workout. */
    fun repeat() {
        viewModelScope.launch {
            val started = startWorkout.repeat(id)
            local.update { it.copy(openWorkout = started) }
        }
    }

    fun onWorkoutOpened() = local.update { it.copy(openWorkout = null) }

    /** Saves the exercises as a new routine, which this workout then counts as done from. */
    fun saveAsRoutine(name: String) {
        val workout = stored.value?.workout ?: return
        val routine = routineFrom(workout, name.trim(), ids)
        if (routine.name.isEmpty()) return
        work.submit {
            routines.create(routine)
            workouts.edit(id) { current ->
                current.copy(
                    routineId = routine.id,
                    exercises = current.exercises.mapIndexed { index, exercise ->
                        exercise.copy(routineExerciseId = routine.exercises.getOrNull(index)?.id)
                    },
                )
            }
        }
        local.update { it.copy(routineDone = RoutineDone.Saved(routine.name)) }
    }

    /** Makes the routine match this workout's exercises, order and set counts. */
    fun updateRoutine() {
        val workout = stored.value?.workout
        val routineId = workout?.routineId
        val offer = (uiState.value as? WorkoutDetailUiState.Ready)?.routine as? RoutineOffer.Update
        if (workout == null || routineId == null || offer == null) return
        work.submit { routines.edit(routineId) { it.updatedFrom(workout, ids) } }
        local.update { it.copy(routineDone = RoutineDone.Updated(offer.name)) }
    }

    fun delete() = work.submit { workouts.delete(id) }

    private fun state(inputs: DetailInputs, extras: DetailExtras): WorkoutDetailUiState {
        val stored = inputs.stored
        val workout = stored?.workout
        return when {
            stored == null -> WorkoutDetailUiState.Loading

            workout == null -> WorkoutDetailUiState.Gone

            else -> detailState(
                workout,
                inputs.routine,
                inputs.activeId,
                inputs.settings,
                inputs.local,
                route.justFinished,
                ZoneId.systemDefault(),
                extras,
            )
        }
    }

    private class DetailInputs(
        val stored: Stored?,
        val routine: Routine?,
        val activeId: WorkoutId?,
        val settings: UserSettings,
        val local: DetailLocalState,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
