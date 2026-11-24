package io.github.amibhai.liftlog.feature.exercises.progress

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.ProgressMetric
import io.github.amibhai.liftlog.core.domain.progress.ProgressRange
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.FakeProgressRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ExerciseProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bench = ExerciseId("bench")
    private val now = 1_700_000_000_000L
    private val progress = FakeProgressRepository()

    @Test
    fun theChartStartsWithTheMostUsefulMetricOverAllTime() = runTest {
        progress.sessions.value = mapOf(bench to listOf(session(400, 100.0, 5), session(10, 105.0, 5)))
        val viewModel = viewModel()

        val state = viewModel.uiState.value!!

        assertThat(state.metric).isEqualTo(ProgressMetric.E1RM)
        assertThat(state.points).hasSize(2)
        assertThat(state.sessionCount).isEqualTo(2)
        assertThat(state.marks.map { it.value }).contains(105.0)
        assertThat(state.repMaxes.first { it.reps == 5 }.weightKg).isEqualTo(105.0)
    }

    @Test
    fun theMetricAndRangeCanBeChosen() = runTest {
        progress.sessions.value = mapOf(bench to listOf(session(400, 100.0, 5), session(10, 105.0, 5)))
        val viewModel = viewModel()

        viewModel.selectMetric(ProgressMetric.TOP_WEIGHT)
        viewModel.selectRange(ProgressRange.MONTH)

        val state = viewModel.uiState.value!!
        assertThat(state.points.map { it.value }).containsExactly(105.0)
    }

    @Test
    fun aMetricWithoutAValueIsNotOffered() = runTest {
        // Twelve reps are too many for an estimate, so there is nothing to chart for it.
        progress.sessions.value = mapOf(bench to listOf(session(5, 60.0, 20)))
        val viewModel = viewModel()

        assertThat(viewModel.uiState.value!!.metrics).doesNotContain(ProgressMetric.E1RM)
        assertThat(viewModel.uiState.value!!.metric).isEqualTo(ProgressMetric.TOP_WEIGHT)
    }

    private fun session(daysAgo: Int, kg: Double, reps: Int) = ExerciseSession(
        workoutId = WorkoutId("w$daysAgo"),
        at = now - TimeUnit.DAYS.toMillis(daysAgo.toLong()),
        sets = listOf(LoggedSet(SetType.WORKING, SetValues(weight = Weight(kg), reps = reps))),
    )

    private fun TestScope.viewModel(): ExerciseProgressViewModel {
        val detail = ExerciseDetail(
            id = bench,
            name = "Bench Press",
            aliases = emptyList(),
            type = ExerciseType.WEIGHT_REPS,
            equipment = Equipment.BARBELL,
            category = ExerciseCategory.STRENGTH,
            mechanic = null,
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            instructions = emptyList(),
            images = emptyList(),
            isUnilateral = false,
            isCustom = false,
            isFavorite = false,
            isArchived = false,
            notes = null,
            attribution = null,
            defaults = io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults(),
        )
        val viewModel = ExerciseProgressViewModel(
            savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "bench")),
            exercises = FakeExerciseRepository(details = mapOf(bench to detail)),
            progress = progress,
            settings = FakeSettingsRepository(),
            clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC),
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }
}
