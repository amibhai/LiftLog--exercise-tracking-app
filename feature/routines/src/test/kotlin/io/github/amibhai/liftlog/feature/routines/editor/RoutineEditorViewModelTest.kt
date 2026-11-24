package io.github.amibhai.liftlog.feature.routines.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.testing.FakeRoutineRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutineEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bench = ExerciseInfo(ExerciseId("bench"), "Bench Press", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
    private val row = ExerciseInfo(ExerciseId("row"), "Barbell Row", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
    private val first = RoutineExerciseId("1")
    private val id = RoutineId("push")
    private val routines = FakeRoutineRepository(
        Routine(
            id,
            "Push",
            exercises = listOf(RoutineExercise(first, bench), RoutineExercise(RoutineExerciseId("2"), row)),
        ),
    )
    private val stored get() = routines.all.single()

    @Test
    fun everyChangeIsStoredRightAway() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(RoutineEditorAction.SetSets(first, 5))
        viewModel.onAction(RoutineEditorAction.SetReps(first, 8, 12))
        viewModel.onAction(RoutineEditorAction.SetEffort(first, 8.5))
        viewModel.onAction(RoutineEditorAction.SetRest(first, 90))
        viewModel.onAction(RoutineEditorAction.Rename("  Push A "))

        val exercise = stored.exercises.first()
        assertThat(exercise.targetSets).isEqualTo(5)
        assertThat(exercise.repMin to exercise.repMax).isEqualTo(8 to 12)
        assertThat(exercise.targetRpe).isEqualTo(8.5)
        assertThat(exercise.restSeconds).isEqualTo(90)
        assertThat(stored.name).isEqualTo("Push A")
    }

    @Test
    fun aRemovedExerciseComesBackWithUndo() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(RoutineEditorAction.Remove(first))
        assertThat(stored.exercises.map { it.exercise.name }).containsExactly("Barbell Row")
        assertThat((viewModel.uiState.value as RoutineEditorUiState.Ready).message)
            .isEqualTo(RoutineEditorMessage.EXERCISE_REMOVED)

        viewModel.onAction(RoutineEditorAction.Undo)
        assertThat(stored.exercises.map { it.exercise.name }).containsExactly("Bench Press", "Barbell Row").inOrder()
    }

    @Test
    fun supersetsAreLettered() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(RoutineEditorAction.Superset(first, setOf(RoutineExerciseId("2"))))

        val exercises = (viewModel.uiState.value as RoutineEditorUiState.Ready).exercises
        assertThat(exercises.map { it.supersetLetter }).containsExactly('A', 'A')
    }

    @Test
    fun aNewRoutineLeftEmptyIsDeleted() = runTest {
        val routines = FakeRoutineRepository(Routine(id, name = ""))
        val store = ViewModelStore()
        provide(store, routines)

        store.clear()

        assertThat(routines.all).isEmpty()
    }

    @Test
    fun aNamedRoutineIsKeptWhenLeft() = runTest {
        val routines = FakeRoutineRepository(Routine(id, name = ""))
        val store = ViewModelStore()
        provide(store, routines).onAction(RoutineEditorAction.Rename("Legs"))

        store.clear()

        assertThat(routines.all.map { it.name }).containsExactly("Legs")
    }

    private fun TestScope.viewModel(): RoutineEditorViewModel = provide(ViewModelStore(), routines)

    private fun TestScope.provide(store: ViewModelStore, routines: FakeRoutineRepository): RoutineEditorViewModel {
        val work = SequentialWork(CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher))
        val factory = viewModelFactory {
            initializer {
                RoutineEditorViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("routineId" to id.value)),
                    routines = routines,
                    work = work,
                    settings = FakeSettingsRepository(),
                )
            }
        }
        val viewModel = ViewModelProvider.create(store, factory)[RoutineEditorViewModel::class]
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }
}
