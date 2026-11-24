package io.github.amibhai.liftlog.feature.progress

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.MuscleLoad
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class ProgressMappingTest {

    /** A Tuesday. */
    private val today = LocalDate.of(2026, 11, 24)

    @Test
    fun theWeekStartsOnTheChosenDayAndCountsItsWorkouts() {
        val state = dashboard(
            workout(today, sets = 10),
            workout(today.minusDays(1), sets = 6),
            workout(today.minusDays(2)),
        )

        assertThat(state.thisWeek.workouts).isEqualTo(2)
        assertThat(state.thisWeek.sets).isEqualTo(16)
        assertThat(state.weeks).hasSize(CHART_WEEKS)
        // Last Sunday belongs to the week before, so this is the second week in a row.
        assertThat(state.streakWeeks).isEqualTo(2)
    }

    @Test
    fun theCalendarHasAColumnPerWeekAndNothingForTheFuture() {
        val state = dashboard(workout(today, sets = 12))

        assertThat(state.calendar).hasSize(CALENDAR_WEEKS)
        val thisWeek = state.calendar.last()
        assertThat(thisWeek.start).isEqualTo(LocalDate.of(2026, 11, 23))
        assertThat(thisWeek.sets).containsExactly(0, 12, null, null, null, null, null).inOrder()
    }

    @Test
    fun musclesAreComparedWithTheWeeklyTarget() {
        val work = MuscleWork(ExerciseId("bench"), listOf(Muscle.CHEST), listOf(Muscle.TRICEPS), sets = 12)

        val state = dashboard(workout(today), work = listOf(work))

        val chest = state.muscles.first()
        assertThat(chest).isEqualTo(MuscleRow(Muscle.CHEST, 12.0, MuscleLoad.WITHIN))
        assertThat(state.muscles.first { it.muscle == Muscle.TRICEPS }.load).isEqualTo(MuscleLoad.BELOW)
        assertThat(state.muscles.last().load).isEqualTo(MuscleLoad.NONE)
        assertThat(state.levels[Muscle.CHEST]).isEqualTo(1f)
        assertThat(state.levels[Muscle.TRICEPS]).isWithin(0.001f).of(0.6f)
    }

    @Test
    fun secondaryMusclesCountAsSettingsSay() {
        val work = MuscleWork(ExerciseId("bench"), listOf(Muscle.CHEST), listOf(Muscle.TRICEPS), sets = 12)

        val state = dashboard(workout(today), work = listOf(work), settings = UserSettings(secondaryMuscleSets = 0.0))

        assertThat(state.levels).doesNotContainKey(Muscle.TRICEPS)
    }

    @Test
    fun theLatestBodyWeightIsShown() {
        val weight = BodyMetric(BodyMetricId("1"), BodyMetricType.BODY_WEIGHT, 81.0, 5)

        val state = dashboard(workout(today), metrics = listOf(weight))

        assertThat(state.bodyWeight).isEqualTo(weight)
        assertThat(dashboard().hasWorkouts).isFalse()
    }

    private fun dashboard(
        vararg workouts: WorkoutStats,
        work: List<MuscleWork> = emptyList(),
        metrics: List<BodyMetric> = emptyList(),
        settings: UserSettings = UserSettings(),
    ) = dashboard(
        data = DashboardData(workouts.toList(), work, records = emptyList(), metrics = metrics),
        settings = settings,
        today = today,
        firstDay = DayOfWeek.MONDAY,
        zone = ZoneOffset.UTC,
    )

    private fun workout(day: LocalDate, sets: Int = 5): WorkoutStats {
        val start = day.atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return WorkoutStats(WorkoutId(day.toString()), start, start + 3_600_000, sets, 1_000.0)
    }
}
