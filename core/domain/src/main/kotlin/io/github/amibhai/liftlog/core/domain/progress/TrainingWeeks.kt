package io.github.amibhai.liftlog.core.domain.progress

import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

private const val MILLIS_PER_SECOND = 1_000L

/** A finished workout, summed up for the dashboard. */
data class WorkoutStats(
    val id: WorkoutId,
    val startedAt: Long,
    val endedAt: Long,
    val sets: Int,
    val volumeKg: Double,
) {
    val seconds: Long get() = (endedAt - startedAt).coerceAtLeast(0) / MILLIS_PER_SECOND

    fun day(zone: ZoneId): LocalDate = Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()
}

/** The first day of the week that [date] is in. */
fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDay))

/**
 * Weeks in a row with at least one workout (SPEC §8). A week without one breaks the streak only once
 * it is over, so this week never does.
 */
fun weeklyStreak(days: Collection<LocalDate>, today: LocalDate, firstDay: DayOfWeek): Int {
    val weeks = days.map { weekStart(it, firstDay) }.toSet()
    val current = weekStart(today, firstDay)
    val last = if (current in weeks) current else current.minusWeeks(1)
    return generateSequence(last) { it.minusWeeks(1) }.takeWhile { it in weeks }.count()
}

/** One week of training. */
data class WeekTotals(
    val start: LocalDate,
    val workouts: Int = 0,
    val sets: Int = 0,
    val volumeKg: Double = 0.0,
    val seconds: Long = 0,
)

/** The last [weeks] weeks up to the one of [today], oldest first; weeks without training are zeros. */
fun weeklyTotals(
    stats: List<WorkoutStats>,
    weeks: Int,
    today: LocalDate,
    firstDay: DayOfWeek,
    zone: ZoneId,
): List<WeekTotals> {
    val byWeek = stats.groupBy { weekStart(it.day(zone), firstDay) }
    val current = weekStart(today, firstDay)
    return (weeks - 1 downTo 0).map { back ->
        val start = current.minusWeeks(back.toLong())
        val done = byWeek[start].orEmpty()
        WeekTotals(
            start = start,
            workouts = done.size,
            sets = done.sumOf { it.sets },
            volumeKg = done.sumOf { it.volumeKg },
            seconds = done.sumOf { it.seconds },
        )
    }
}

/** How much was done on each day with a workout, for the calendar heatmap: sets done. */
fun setsPerDay(stats: List<WorkoutStats>, zone: ZoneId): Map<LocalDate, Int> =
    stats.groupingBy { it.day(zone) }.fold(0) { sum, workout -> sum + workout.sets }
