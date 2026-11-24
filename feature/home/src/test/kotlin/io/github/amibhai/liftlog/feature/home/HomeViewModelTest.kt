package io.github.amibhai.liftlog.feature.home

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.FakeHistoryRepository
import io.github.amibhai.liftlog.core.testing.FakeRoutineRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = 1_700_000_000_000L
    private val bench = ExerciseInfo(ExerciseId("bench"), "Bench Press", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
    private val row = ExerciseInfo(ExerciseId("row"), "Barbell Row", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
    private val push = Routine(
        RoutineId("push"),
        "Push",
        exercises = listOf(RoutineExercise(RoutineExerciseId("r"), bench)),
    )
    private val routines = FakeRoutineRepository(push)

    @Test
    fun repeatLastStartsTheRoutineOfTheLastWorkout() = runTest {
        val workouts = FakeWorkoutRepository(lastWorkout(routineId = push.id))
        val viewModel = viewModel(workouts)
        assertThat(viewModel.uiState.value.repeatLast?.routineName).isEqualTo("Push")

        viewModel.repeatLast()

        val started = workouts.active!!
        assertThat(started.routineId).isEqualTo(push.id)
        assertThat(started.exercises.map { it.exercise.name }).containsExactly("Bench Press")
        assertThat(viewModel.uiState.value.openWorkout).isEqualTo(started.id)
    }

    @Test
    fun withoutARoutineTheLastWorkoutItselfIsRepeated() = runTest {
        val workouts = FakeWorkoutRepository(lastWorkout(routineId = null))
        val viewModel = viewModel(workouts)
        assertThat(viewModel.uiState.value.repeatLast?.routineName).isNull()

        viewModel.repeatLast()

        assertThat(workouts.active!!.exercises.map { it.exercise.name }).containsExactly("Barbell Row")
    }

    @Test
    fun aNewRoutineOpensOnceItIsStored() = runTest {
        val viewModel = viewModel(FakeWorkoutRepository())

        viewModel.newRoutine()

        val created = routines.all.last()
        assertThat(created.name).isEmpty()
        assertThat(viewModel.uiState.value.openRoutine).isEqualTo(created.id)
        viewModel.onRoutineOpened()
        assertThat(viewModel.uiState.value.openRoutine).isNull()
    }

    @Test
    fun aDeletedRoutineCanBeBroughtBack() = runTest {
        val viewModel = viewModel(FakeWorkoutRepository())

        viewModel.deleteRoutine(push.id)
        assertThat(routines.all).isEmpty()
        assertThat(viewModel.uiState.value.deletedRoutine).isEqualTo(push.id)

        viewModel.undoDeleteRoutine()
        assertThat(routines.all.map { it.id }).containsExactly(push.id)
        assertThat(viewModel.uiState.value.deletedRoutine).isNull()
    }

    @Test
    fun theWorkoutInProgressIsKnown() = runTest {
        val workouts = FakeWorkoutRepository(Workout(WorkoutId("active"), startedAt = now))

        assertThat(viewModel(workouts).uiState.value.activeWorkoutId).isEqualTo(WorkoutId("active"))
    }

    private fun lastWorkout(routineId: RoutineId?) = Workout(
        id = WorkoutId("last"),
        startedAt = now - HOUR,
        endedAt = now,
        isInProgress = false,
        routineId = routineId,
        exercises = listOf(
            WorkoutExercise(
                id = WorkoutExerciseId("e"),
                exercise = if (routineId == null) row else bench,
                sets = listOf(
                    WorkoutSet(
                        id = WorkoutSetId("s"),
                        entered = SetValues(weight = Weight(60.0), reps = 8),
                        isCompleted = true,
                        completedAt = now,
                    ),
                ),
            ),
        ),
    )

    private fun TestScope.viewModel(workouts: FakeWorkoutRepository): HomeViewModel {
        var next = 0
        val ids = IdGenerator { "id${next++}" }
        val clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)
        val viewModel = HomeViewModel(
            exercises = FakeExerciseRepository(),
            workouts = workouts,
            routines = routines,
            startWorkout = StartWorkout(workouts, routines, FakeHistoryRepository(), WorkoutFactory(ids), clock),
            ids = ids,
            work = SequentialWork(CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher)),
            clock = clock,
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private companion object {
        const val HOUR = 3_600_000L
    }
}
