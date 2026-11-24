package io.github.amibhai.liftlog.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.PersonalRecord
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.RecordType
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.usecase.ProgressionAdvisor
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import io.github.amibhai.liftlog.core.testing.FakeHistoryRepository
import io.github.amibhai.liftlog.core.testing.FakeProgramRepository
import io.github.amibhai.liftlog.core.testing.FakeProgressRepository
import io.github.amibhai.liftlog.core.testing.FakeRoutineRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import io.github.amibhai.liftlog.feature.history.HOUR
import io.github.amibhai.liftlog.feature.history.NOW
import io.github.amibhai.liftlog.feature.history.bench
import io.github.amibhai.liftlog.feature.history.done
import io.github.amibhai.liftlog.feature.history.exercise
import io.github.amibhai.liftlog.feature.history.finished
import io.github.amibhai.liftlog.feature.history.squat
import io.github.amibhai.liftlog.feature.history.testClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val id = WorkoutId("w")
    private val progress = FakeProgressRepository()
    private val workout = finished(
        "w",
        NOW - HOUR,
        exercise("1", bench, done("s1", 80.0, 8), done("s2", 80.0, 8)),
        exercise("2", squat, done("s3", 100.0, 5)),
    )
    private val workouts = FakeWorkoutRepository(workout)
    private val routines = FakeRoutineRepository()

    @Test
    fun savingAsARoutineLinksTheWorkoutToIt() = runTest {
        val viewModel = viewModel()
        assertThat(ready(viewModel).routine).isEqualTo(RoutineOffer.Save)

        viewModel.saveAsRoutine("  Push ")

        val routine = routines.all.single()
        assertThat(routine.name).isEqualTo("Push")
        assertThat(routine.exercises.map { it.exercise.name }).containsExactly("Bench Press", "Squat").inOrder()
        assertThat(routine.exercises[0].targetSets).isEqualTo(2)
        val linked = workouts[id]!!
        assertThat(linked.routineId).isEqualTo(routine.id)
        assertThat(linked.exercises.map { it.routineExerciseId }).isEqualTo(routine.exercises.map { it.id })
        assertThat(ready(viewModel).routine).isEqualTo(RoutineOffer.None)
        assertThat(ready(viewModel).routineDone).isEqualTo(RoutineDone.Saved("Push"))
    }

    @Test
    fun aWorkoutThatWentBeyondItsRoutineOffersToUpdateIt() = runTest {
        val slot = RoutineExercise(RoutineExerciseId("r1"), bench, targetSets = 2)
        val push = Routine(RoutineId("push"), "Push", exercises = listOf(slot))
        val routines = FakeRoutineRepository(push)
        val workouts = FakeWorkoutRepository(
            workout.copy(
                routineId = push.id,
                exercises = listOf(workout.exercises[0].copy(routineExerciseId = slot.id), workout.exercises[1]),
            ),
        )
        val viewModel = viewModel(workouts, routines)
        assertThat(ready(viewModel).routine).isEqualTo(RoutineOffer.Update("Push"))

        viewModel.updateRoutine()

        assertThat(routines.all.single().exercises.map { it.exercise.name }).containsExactly("Bench Press", "Squat")
        assertThat(routines.all.single().exercises[0].id).isEqualTo(slot.id)
        assertThat(ready(viewModel).routine).isEqualTo(RoutineOffer.None)
    }

    @Test
    fun repeatingStartsTheSameExercisesAgain() = runTest {
        val viewModel = viewModel()

        viewModel.repeat()

        val started = ready(viewModel).openWorkout
        assertThat(started).isNotNull()
        assertThat(workouts.active?.id).isEqualTo(started)
        assertThat(workouts.active?.exercises?.map { it.exercise.name }).containsExactly("Bench Press", "Squat")
        viewModel.onWorkoutOpened()
        assertThat(ready(viewModel).openWorkout).isNull()
    }

    @Test
    fun theRecordsAndMusclesOfTheWorkoutAreShown() = runTest {
        progress.recordsOf.value = mapOf(
            id to listOf(
                RecordEntry(
                    record = PersonalRecord(
                        exerciseId = bench.id,
                        type = RecordType.HEAVIEST_WEIGHT,
                        value = 80.0,
                        weightKg = 80.0,
                        reps = 8,
                        workoutId = id,
                        setId = null,
                        achievedAt = NOW,
                    ),
                    exerciseName = "Bench Press",
                    exerciseType = ExerciseType.WEIGHT_REPS,
                ),
            ),
        )
        progress.workOf.value = mapOf(
            id to listOf(MuscleWork(bench.id, listOf(Muscle.CHEST), listOf(Muscle.TRICEPS), sets = 2)),
        )
        val state = ready(viewModel())

        assertThat(state.records.single().exerciseName).isEqualTo("Bench Press")
        assertThat(state.muscles.map { it.muscle to it.sets }).containsExactly(
            Muscle.CHEST to 2.0,
            Muscle.TRICEPS to 1.0,
        ).inOrder()
        assertThat(state.levels[Muscle.CHEST]).isEqualTo(1f)
    }

    @Test
    fun deletingClosesTheScreen() = runTest {
        val viewModel = viewModel()

        viewModel.delete()

        assertThat(viewModel.uiState.value).isEqualTo(WorkoutDetailUiState.Gone)
    }

    private fun TestScope.viewModel(
        workouts: FakeWorkoutRepository = this@WorkoutDetailViewModelTest.workouts,
        routines: FakeRoutineRepository = this@WorkoutDetailViewModelTest.routines,
    ): WorkoutDetailViewModel {
        var next = 0
        val ids = IdGenerator { "id${next++}" }
        val history = FakeHistoryRepository()
        val viewModel = WorkoutDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("workoutId" to "w", "justFinished" to false)),
            workouts = workouts,
            routines = routines,
            progress = progress,
            startWorkout = StartWorkout(
                workouts = workouts,
                routines = routines,
                programs = FakeProgramRepository(),
                history = history,
                advisor = ProgressionAdvisor(progress, FakeSettingsRepository()),
                factory = WorkoutFactory(ids),
                clock = testClock,
            ),
            ids = ids,
            work = SequentialWork(CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher)),
            settings = FakeSettingsRepository(),
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun ready(viewModel: WorkoutDetailViewModel) = viewModel.uiState.value as WorkoutDetailUiState.Ready
}
