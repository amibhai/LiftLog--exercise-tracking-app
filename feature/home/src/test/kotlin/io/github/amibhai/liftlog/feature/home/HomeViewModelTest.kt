package io.github.amibhai.liftlog.feature.home

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.PersonalRecord
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.RecordType
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
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.usecase.ProgressionAdvisor
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.FakeHistoryRepository
import io.github.amibhai.liftlog.core.testing.FakeProgramRepository
import io.github.amibhai.liftlog.core.testing.FakeProgressRepository
import io.github.amibhai.liftlog.core.testing.FakeRoutineRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
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
    private val progress = FakeProgressRepository()
    private val programs = FakeProgramRepository()

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
    fun theNextDayOfTheFollowedProgramIsSuggestedAndCounts() = runTest {
        val day = ProgramDay(ProgramDayId("d"), "Push day", push.id)
        programs.create(Program(ProgramId("p"), "My split", isActive = true, days = listOf(day)))
        val workouts = FakeWorkoutRepository()
        val viewModel = viewModel(workouts)

        val next = viewModel.uiState.value.next
        assertThat(next).isEqualTo(NextWorkout("My split", "Push day", push.id, listOf("Bench Press")))

        viewModel.startRoutine(push.id)
        assertThat(workouts.active!!.programDayId).isEqualTo(day.id)
    }

    @Test
    fun theWorkoutInProgressIsKnown() = runTest {
        val workouts = FakeWorkoutRepository(Workout(WorkoutId("active"), startedAt = now))

        assertThat(viewModel(workouts).uiState.value.activeWorkoutId).isEqualTo(WorkoutId("active"))
    }

    @Test
    fun thisWeekAndTheLatestRecordsAreShown() = runTest {
        progress.stats.value = listOf(
            WorkoutStats(WorkoutId("now"), startedAt = now - HOUR, endedAt = now, sets = 12, volumeKg = 4_000.0),
            WorkoutStats(WorkoutId("before"), now - WEEK - HOUR, now - WEEK, sets = 9, volumeKg = 3_000.0),
        )
        progress.recentRecords.value = List(4) { record(RecordType.entries[it]) }

        val training = viewModel(FakeWorkoutRepository()).uiState.value.training!!
        assertThat(training.week.workouts).isEqualTo(1)
        assertThat(training.week.sets).isEqualTo(12)
        assertThat(training.streakWeeks).isEqualTo(2)
        assertThat(training.records).hasSize(3)
    }

    @Test
    fun thereAreNoStatisticsBeforeTheFirstWorkout() = runTest {
        assertThat(viewModel(FakeWorkoutRepository()).uiState.value.training).isNull()
    }

    private fun record(type: RecordType) = RecordEntry(
        record = PersonalRecord(
            exerciseId = bench.id,
            type = type,
            value = 80.0,
            weightKg = 80.0,
            reps = 8,
            workoutId = WorkoutId("now"),
            setId = null,
            achievedAt = now,
        ),
        exerciseName = bench.name,
        exerciseType = bench.type,
    )

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
        val settings = FakeSettingsRepository()
        val advisor = ProgressionAdvisor(progress, settings)
        val factory = WorkoutFactory(ids)
        val viewModel = HomeViewModel(
            exercises = FakeExerciseRepository(),
            workouts = workouts,
            routines = routines,
            startWorkout = StartWorkout(workouts, routines, programs, FakeHistoryRepository(), advisor, factory, clock),
            programs = programs,
            progress = progress,
            settings = settings,
            ids = ids,
            work = SequentialWork(CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher)),
            clock = clock,
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val WEEK = 7 * 24 * HOUR
    }
}
