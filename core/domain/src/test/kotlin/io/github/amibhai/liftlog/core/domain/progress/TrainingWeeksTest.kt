package io.github.amibhai.liftlog.core.domain.progress

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class TrainingWeeksTest {

    /** A Tuesday. */
    private val today = LocalDate.of(2026, 11, 24)
    private val monday = DayOfWeek.MONDAY

    @Test
    fun `a week starts on the chosen day`() {
        assertThat(weekStart(today, DayOfWeek.MONDAY)).isEqualTo(LocalDate.of(2026, 11, 23))
        assertThat(weekStart(today, DayOfWeek.SUNDAY)).isEqualTo(LocalDate.of(2026, 11, 22))
        assertThat(weekStart(today, DayOfWeek.TUESDAY)).isEqualTo(today)
    }

    @Test
    fun `weeks with a workout in a row make the streak`() {
        val days = listOf(today, today.minusWeeks(1), today.minusWeeks(2), today.minusWeeks(4))

        assertThat(weeklyStreak(days, today, monday)).isEqualTo(3)
    }

    @Test
    fun `this week does not break the streak before it is over`() {
        val days = listOf(today.minusWeeks(1), today.minusWeeks(2))

        assertThat(weeklyStreak(days, today, monday)).isEqualTo(2)
        assertThat(weeklyStreak(emptyList(), today, monday)).isEqualTo(0)
    }

    @Test
    fun `weekly totals include weeks without training`() {
        val stats = listOf(
            workout("a", today, hour = 18, sets = 10, kg = 1_000.0),
            workout("b", today.minusDays(1), hour = 9, sets = 5, kg = 500.0),
            workout("c", today.minusWeeks(2), hour = 18, sets = 8, kg = 800.0),
        )

        val weeks = weeklyTotals(stats, weeks = 3, today = today, firstDay = monday, zone = ZoneOffset.UTC)

        assertThat(weeks.map { it.workouts }).containsExactly(1, 0, 2).inOrder()
        assertThat(weeks.last().start).isEqualTo(LocalDate.of(2026, 11, 23))
        assertThat(weeks.last().sets).isEqualTo(15)
        assertThat(weeks.last().volumeKg).isEqualTo(1_500.0)
        assertThat(weeks.last().seconds).isEqualTo(2 * 3_600)
    }

    @Test
    fun `every training day is counted with its sets`() {
        val stats = listOf(
            workout("a", today, hour = 8, sets = 4, kg = 0.0),
            workout("b", today, hour = 20, sets = 6, kg = 0.0),
        )

        assertThat(setsPerDay(stats, ZoneOffset.UTC)).containsExactly(today, 10)
    }

    private fun workout(id: String, day: LocalDate, hour: Int, sets: Int, kg: Double): WorkoutStats {
        val start = day.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return WorkoutStats(WorkoutId(id), start, start + 3_600_000, sets, kg)
    }
}
