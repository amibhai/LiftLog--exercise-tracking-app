package io.github.amibhai.liftlog

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
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
import io.github.amibhai.liftlog.core.ui.launch.LaunchAction
import io.github.amibhai.liftlog.ui.AppTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class MainActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    @Test
    fun theFirstRunShowsTheOnboardingUntilItIsDone() = runTest {
        val viewModel = viewModel(FakeWorkoutRepository())
        assertThat(ready(viewModel).showOnboarding).isTrue()

        settings.update { it.copy(onboardingDone = true) }
        viewModel.onOnboardingFinished(openPrograms = true)

        assertThat(ready(viewModel).showOnboarding).isFalse()
        assertThat(viewModel.target.value).isEqualTo(AppTarget.Programs)
        viewModel.onTargetOpened()
        assertThat(viewModel.target.value).isNull()
    }

    @Test
    fun whoeverHasTrainedBeforeDoesNotSeeIt() = runTest {
        val done = Workout(id = WorkoutId("w"), startedAt = 1, endedAt = 2, isInProgress = false)
        val viewModel = viewModel(FakeWorkoutRepository(done))

        assertThat(ready(viewModel).showOnboarding).isFalse()
        assertThat(settings.settings.first().onboardingDone).isTrue()
    }

    @Test
    fun aShortcutStartsAWorkoutAndOpensIt() = runTest {
        val workouts = FakeWorkoutRepository()
        val viewModel = viewModel(workouts)

        viewModel.onLaunch(LaunchAction.REPEAT_LAST)

        assertThat(viewModel.target.value).isEqualTo(AppTarget.ActiveWorkout(workouts.active!!.id))
    }

    private fun ready(viewModel: MainActivityViewModel) = viewModel.uiState.value as MainUiState.Ready

    private fun viewModel(workouts: FakeWorkoutRepository): MainActivityViewModel {
        var next = 0
        val ids = IdGenerator { "id${next++}" }
        val start = StartWorkout(
            workouts = workouts,
            routines = FakeRoutineRepository(),
            programs = FakeProgramRepository(),
            history = FakeHistoryRepository(),
            advisor = ProgressionAdvisor(FakeProgressRepository(), settings),
            factory = WorkoutFactory(ids),
            clock = Clock.systemUTC(),
        )
        return MainActivityViewModel(settings, workouts, start)
    }
}
