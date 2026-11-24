package io.github.amibhai.liftlog.feature.exercises.picker

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.usecase.AddExercises
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.FakeHistoryRepository
import io.github.amibhai.liftlog.core.testing.FakeRoutineRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import io.github.amibhai.liftlog.feature.exercises.NOW
import io.github.amibhai.liftlog.feature.exercises.browser
import io.github.amibhai.liftlog.feature.exercises.catalogExercise
import io.github.amibhai.liftlog.feature.exercises.testClock
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExercisePickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var nextId = 0
    private val exercises = FakeExerciseRepository(listOf("Bench", "Row", "Curl").map { catalogExercise(it) })
    private val workouts = FakeWorkoutRepository(Workout(WorkoutId("w"), startedAt = NOW))
    private val routines = FakeRoutineRepository(Routine(RoutineId("r"), "Push"))
    private val bench = ExerciseId("Bench")
    private val row = ExerciseId("Row")

    @Test
    fun exercisesAreAddedInTheOrderTheyWerePicked() = runTest {
        val viewModel = viewModel(workout = "w")

        viewModel.onPick(row)
        viewModel.onPick(bench)
        viewModel.onPick(ExerciseId("Curl"))
        viewModel.onPick(ExerciseId("Curl"))
        viewModel.add(asSuperset = true)

        val added = workouts[WorkoutId("w")]!!.exercises
        assertThat(added.map { it.exercise.name }).containsExactly("Row", "Bench").inOrder()
        assertThat(added.map { it.supersetGroup }).containsExactly(1, 1)
        viewModel.uiState.test { assertThat(expectMostRecentItem().isDone).isTrue() }
    }

    @Test
    fun replacingTakesOneTap() = runTest {
        viewModel(workout = "w").apply {
            onPick(bench)
            add(asSuperset = false)
        }
        val slot = workouts[WorkoutId("w")]!!.exercises.single().id.value

        viewModel(workout = "w", slot = slot).onPick(row)

        assertThat(workouts[WorkoutId("w")]!!.exercises.single().exercise.name).isEqualTo("Row")
    }

    @Test
    fun routinesCanBeExtendedToo() = runTest {
        viewModel(routine = "r").apply {
            onPick(bench)
            add(asSuperset = false)
        }

        assertThat(routines.all.single().exercises.single().exercise.name).isEqualTo("Bench")
    }

    @Test
    fun aNewCustomExerciseIsSelected() = runTest {
        val viewModel = viewModel(workout = "w")

        viewModel.onCreated(row)
        viewModel.onCreated(row)

        viewModel.uiState.test { assertThat(expectMostRecentItem().selected).containsExactly(row) }
    }

    private fun viewModel(
        workout: String? = null,
        routine: String? = null,
        slot: String? = null,
    ): ExercisePickerViewModel {
        val ids = IdGenerator { "id${nextId++}" }
        return ExercisePickerViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf("workoutId" to workout, "routineId" to routine, "slotId" to slot),
            ),
            browser = browser(exercises, mainDispatcherRule.dispatcher),
            workouts = workouts,
            routines = routines,
            addExercises = AddExercises(workouts, routines, exercises, FakeHistoryRepository(), ids, testClock),
        )
    }
}
