package io.github.amibhai.liftlog.feature.workout.logging

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.progression.PlannedSession
import io.github.amibhai.liftlog.core.domain.progression.SuggestionReason
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.usecase.ProgressionAdvisor
import io.github.amibhai.liftlog.core.testing.FakeExerciseEditor
import io.github.amibhai.liftlog.core.testing.FakeHistoryRepository
import io.github.amibhai.liftlog.core.testing.FakeProgressRepository
import io.github.amibhai.liftlog.core.testing.FakeRoutineRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import io.github.amibhai.liftlog.core.ui.component.NumberPadKey
import io.github.amibhai.liftlog.core.ui.component.SetRowAction
import io.github.amibhai.liftlog.feature.workout.NOW
import io.github.amibhai.liftlog.feature.workout.bench
import io.github.amibhai.liftlog.feature.workout.exercise
import io.github.amibhai.liftlog.feature.workout.set
import io.github.amibhai.liftlog.feature.workout.testClock
import io.github.amibhai.liftlog.feature.workout.values
import io.github.amibhai.liftlog.feature.workout.workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val workouts = FakeWorkoutRepository(
        workout(exercise("e", bench, set("1", target = values(80.0, 8)), set("2", target = values(80.0, 8)))),
    )
    private val id = WorkoutId("w")
    private val progress = FakeProgressRepository()
    private val settings = FakeSettingsRepository()
    private val routines = FakeRoutineRepository()
    private val stored: Workout get() = workouts[id]!!

    @Test
    fun oneTapLogsTheSetAsShownAndStartsTheRest() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1")))

        val logged = stored.exercises[0].sets[0]
        assertThat(logged.isCompleted).isTrue()
        assertThat(logged.entered).isEqualTo(values(80.0, 8))
        assertThat(stored.restTimer?.endsAt).isEqualTo(NOW + 120_000)
        assertThat(ready(viewModel).focus?.setId).isEqualTo("2")
        assertThat(ready(viewModel).items.map { it.key }).contains("effort-1")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aTickShowsAtOnceBeforeItIsStored() = runTest {
        // Edits wait in the queue until the test runs them, like a slow database.
        val viewModel = viewModel(work = SequentialWork(backgroundScope))
        val row = { ready(viewModel).items.filterIsInstance<WorkoutItem.Set>().first().row }

        viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1")))
        assertThat(row().isCompleted).isTrue()
        assertThat(stored.exercises[0].sets[0].isCompleted).isFalse()

        viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1")))
        assertThat(row().isCompleted).isFalse()

        runCurrent()
        assertThat(stored.exercises[0].sets[0].isCompleted).isFalse()
        assertThat(row().isCompleted).isFalse()
    }

    @Test
    fun aSetThatBeatsTheOldBestIsMarkedAsARecord() = runTest {
        progress.sessions.value = mapOf(
            bench.id to listOf(
                ExerciseSession(
                    workoutId = WorkoutId("old"),
                    at = NOW - 1,
                    sets = listOf(LoggedSet(SetType.WORKING, values(75.0, 8), id = WorkoutSetId("o"))),
                ),
            ),
        )
        val viewModel = viewModel()

        viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1")))
        val rows = ready(viewModel).items.filterIsInstance<WorkoutItem.Set>().map { it.row }
        assertThat(rows[0].isRecord).isTrue()
        assertThat(rows[1].isRecord).isFalse()
    }

    @Test
    fun theProgressionExplainsTheWorkoutOfARoutine() = runTest {
        val slot = RoutineExercise(RoutineExerciseId("slot"), bench, targetSets = 1, repMin = 6, repMax = 8)
        routines.create(Routine(RoutineId("r"), "Push", exercises = listOf(slot)))
        progress.planned.value = mapOf(
            bench.id to listOf(
                PlannedSession(NOW - DAY, listOf(LoggedSet(SetType.WORKING, values(80.0, 8))), slot = slot.id),
            ),
        )
        val planned = exercise("e", bench, set("1", target = values(82.5, 6))).copy(routineExerciseId = slot.id)
        val viewModel = viewModel(FakeWorkoutRepository(workout(planned).copy(routineId = RoutineId("r"))))

        assertThat(ready(viewModel).items.filterIsInstance<WorkoutItem.Header>().single().reason)
            .isEqualTo(SuggestionReason.HeavierAfter(8))
    }

    @Test
    fun tappingACompletedSetOpensItAgain() = runTest {
        val viewModel = viewModel()

        repeat(2) { viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1"))) }

        assertThat(stored.exercises[0].sets[0].isCompleted).isFalse()
        assertThat(ready(viewModel).items.map { it.key }).doesNotContain("effort-1")
    }

    @Test
    fun everyKeyIsStoredRightAway() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WorkoutAction.Row(SetRowAction.EditField("1", SetField.WEIGHT)))
        listOf(1, 0, 0).forEach { viewModel.onAction(WorkoutAction.PadKey(NumberPadKey.Digit(it))) }

        assertThat(stored.exercises[0].sets[0].entered.weight).isEqualTo(Weight(100.0))
        assertThat(ready(viewModel).pad?.display).isEqualTo("100")
    }

    @Test
    fun nextGoesToTheRepsThenToTheNextSet() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WorkoutAction.Row(SetRowAction.EditField("1", SetField.WEIGHT)))
        viewModel.onAction(WorkoutAction.PadKey(NumberPadKey.Next))
        assertThat(ready(viewModel).pad?.field).isEqualTo(SetField.REPS)

        viewModel.onAction(WorkoutAction.PadKey(NumberPadKey.Next))
        val second = ready(viewModel).items.filterIsInstance<WorkoutItem.Set>()[1].row
        assertThat(second.fields.first { it.isFocused }.field).isEqualTo(SetField.WEIGHT)
    }

    @Test
    fun aSetWithoutRepsAsksForThemInsteadOfBeingLogged() = runTest {
        val workouts = FakeWorkoutRepository(workout(exercise("e", bench, set("1", target = values(kg = 80.0)))))
        val viewModel = viewModel(workouts)

        viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1")))

        assertThat(workouts[id]!!.exercises[0].sets[0].isCompleted).isFalse()
        assertThat(ready(viewModel).pad?.field).isEqualTo(SetField.REPS)
    }

    @Test
    fun aDeletedSetComesBackWithUndo() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WorkoutAction.Row(SetRowAction.Delete("1")))
        assertThat(stored.exercises[0].sets.map { it.id.value }).containsExactly("2")
        assertThat(ready(viewModel).message).isEqualTo(WorkoutMessage.SET_DELETED)

        viewModel.onAction(WorkoutAction.Undo)
        assertThat(stored.exercises[0].sets.map { it.id.value }).containsExactly("1", "2").inOrder()
        assertThat(ready(viewModel).message).isNull()
    }

    @Test
    fun finishingKeepsOnlyWhatWasDone() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1")))
        viewModel.onAction(WorkoutAction.Finish)

        assertThat(stored.isInProgress).isFalse()
        assertThat(stored.restTimer).isNull()
        assertThat(stored.exercises[0].sets.map { it.id }).containsExactly(WorkoutSetId("1"))
        assertThat(ready(viewModel).isFinished).isTrue()
    }

    @Test
    fun discardingClosesTheScreen() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(WorkoutAction.Discard)

        assertThat(workouts[id]).isNull()
        assertThat(viewModel.uiState.value).isEqualTo(WorkoutUiState.Gone)
    }

    @Test
    fun theRestCanBeShortenedAndSkipped() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(WorkoutAction.Row(SetRowAction.ToggleComplete("1")))

        viewModel.onAction(WorkoutAction.AdjustRest(-15))
        assertThat(stored.restTimer?.endsAt).isEqualTo(NOW + 105_000)

        viewModel.onAction(WorkoutAction.SkipRest)
        assertThat(stored.restTimer).isNull()
    }

    /** Starts collecting [WorkoutViewModel.uiState] (it is shared while subscribed). */
    private fun TestScope.viewModel(
        repository: FakeWorkoutRepository = workouts,
        work: SequentialWork = SequentialWork(
            CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher),
        ),
    ): WorkoutViewModel {
        var nextId = 0
        val viewModel = WorkoutViewModel(
            savedStateHandle = SavedStateHandle(mapOf("workoutId" to "w")),
            editor = WorkoutEditor(repository, work, IdGenerator { "new${nextId++}" }, testClock),
            pastSessions = PastSessions(
                history = FakeHistoryRepository(),
                progress = progress,
                routines = routines,
                advisor = ProgressionAdvisor(progress, settings),
                clock = testClock,
            ),
            settings = settings,
            exerciseEditor = FakeExerciseEditor(),
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun ready(viewModel: WorkoutViewModel) = viewModel.uiState.value as WorkoutUiState.Ready

    private companion object {
        const val DAY = 24 * 3_600_000L
    }
}
