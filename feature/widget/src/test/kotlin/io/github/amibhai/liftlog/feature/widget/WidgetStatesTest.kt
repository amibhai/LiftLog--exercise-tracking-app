package io.github.amibhai.liftlog.feature.widget

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.ThemeMode
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.testing.FakeProgramRepository
import io.github.amibhai.liftlog.core.testing.FakeProgressRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class WidgetStatesTest {

    private val now = 1_700_000_000_000L
    private val progress = FakeProgressRepository()
    private val programs = FakeProgramRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun withoutAnyTrainingThereIsNothingToShow() = runTest {
        val state = states(FakeWorkoutRepository()).observe().first()

        assertThat(state.active).isNull()
        assertThat(state.nextDay).isNull()
        assertThat(state.streakWeeks).isEqualTo(0)
        assertThat(state.workoutsThisWeek).isEqualTo(0)
    }

    @Test
    fun theWorkoutInProgressIsNamed() = runTest {
        val workouts = FakeWorkoutRepository(Workout(id = WorkoutId("w"), startedAt = now, name = "Push"))

        assertThat(states(workouts).observe().first().active?.name).isEqualTo("Push")
    }

    @Test
    fun theNextDayOfTheProgramIsNamed() = runTest {
        val days = listOf(ProgramDay(ProgramDayId("d"), "Legs", RoutineId("r")))
        programs.create(Program(ProgramId("p"), "PPL", isActive = true, days = days))
        programs.activate(ProgramId("p"))

        assertThat(states(FakeWorkoutRepository()).observe().first().nextDay).isEqualTo("Legs")
    }

    @Test
    fun weeksInARowAndThisWeeksWorkoutsAreCounted() = runTest {
        progress.stats.value = listOf(now - WEEK, now, now - 1).mapIndexed { index, at ->
            WorkoutStats(WorkoutId("w$index"), at, at + 1, sets = 1, volumeKg = 1.0)
        }

        val state = states(FakeWorkoutRepository()).observe().first()
        assertThat(state.streakWeeks).isEqualTo(2)
        assertThat(state.workoutsThisWeek).isEqualTo(2)
    }

    @Test
    fun theThemeOfTheAppIsUsed() = runTest {
        settings.update { it.copy(themeMode = ThemeMode.LIGHT, useDynamicColor = false) }

        val state = states(FakeWorkoutRepository()).observe().first()
        assertThat(state.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(state.useDynamicColor).isFalse()
    }

    private fun states(workouts: FakeWorkoutRepository) =
        WidgetStates(workouts, programs, progress, settings, Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC))

    private companion object {
        val WEEK = TimeUnit.DAYS.toMillis(7)
    }
}
