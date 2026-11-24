package io.github.amibhai.liftlog.feature.exercises.picker

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.usecase.AddExercises
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseBrowser
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseFilter
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseListUiState
import io.github.amibhai.liftlog.feature.exercises.navigation.ExercisePickerDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ExercisePickerUiState(
    val list: ExerciseListUiState = ExerciseListUiState(),
    /** In the order they were picked, which is the order they are added in. */
    val selected: List<ExerciseId> = emptyList(),
    val isReplacing: Boolean = false,
    /** The selection has been added; the screen can close. */
    val isDone: Boolean = false,
)

/** Adds exercises to a workout or a routine, or replaces one of them (SPEC §4). */
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    browser: ExerciseBrowser,
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val addExercises: AddExercises,
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<ExercisePickerDestination>()
    private val workoutId = destination.workoutId?.let(::WorkoutId)
    private val routineId = destination.routineId?.let(::RoutineId)
    private val filter = MutableStateFlow(ExerciseFilter())
    private val selected = MutableStateFlow(emptyList<ExerciseId>())
    private val isDone = MutableStateFlow(false)

    val uiState: StateFlow<ExercisePickerUiState> =
        combine(browser.uiState(filter, currentExercises()), selected, isDone) { list, selected, done ->
            ExercisePickerUiState(list, selected, isReplacing = destination.slotId != null, isDone = done)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            ExercisePickerUiState(isReplacing = destination.slotId != null),
        )

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }

    fun onMuscleChange(muscle: Muscle?) = filter.update { it.copy(muscle = muscle) }

    fun onEquipmentChange(equipment: Equipment?) = filter.update { it.copy(equipment = equipment) }

    /** Selects or deselects; when replacing, the choice is final. */
    fun onPick(id: ExerciseId) {
        if (destination.slotId != null) {
            replace(destination.slotId, id)
        } else {
            selected.update { if (id in it) it - id else it + id }
        }
    }

    /** A custom exercise that was just created is picked right away. */
    fun onCreated(id: ExerciseId) {
        if (id !in selected.value) onPick(id)
    }

    fun add(asSuperset: Boolean) {
        val ids = selected.value
        viewModelScope.launch {
            workoutId?.let { addExercises.toWorkout(it, ids, asSuperset) }
            routineId?.let { addExercises.toRoutine(it, ids, asSuperset) }
            isDone.value = true
        }
    }

    private fun replace(slot: String, id: ExerciseId) {
        viewModelScope.launch {
            workoutId?.let { addExercises.replaceInWorkout(it, WorkoutExerciseId(slot), id) }
            routineId?.let { addExercises.replaceInRoutine(it, RoutineExerciseId(slot), id) }
            isDone.value = true
        }
    }

    private fun currentExercises(): Flow<Set<ExerciseId>> {
        val workout = workoutId?.let(workouts::observe) ?: flowOf(null)
        val routine = routineId?.let(routines::observe) ?: flowOf(null)
        return combine(workout, routine) { inWorkout, inRoutine ->
            val ids = inWorkout?.exercises.orEmpty().map { it.exercise.id } +
                inRoutine?.exercises.orEmpty().map { it.exercise.id }
            ids.toSet()
        }.distinctUntilChanged()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
