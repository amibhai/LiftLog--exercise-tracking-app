package io.github.amibhai.liftlog.feature.progress

import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.domain.progress.muscleLoad
import io.github.amibhai.liftlog.core.domain.progress.muscleSets
import io.github.amibhai.liftlog.core.domain.progress.setsPerDay
import io.github.amibhai.liftlog.core.domain.progress.weekStart
import io.github.amibhai.liftlog.core.domain.progress.weeklyStreak
import io.github.amibhai.liftlog.core.domain.progress.weeklyTotals
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Weeks of charts and of the calendar heatmap. */
internal const val CHART_WEEKS = 12
internal const val CALENDAR_WEEKS = 17
private const val DAYS_PER_WEEK = 7

/** Everything the dashboard shows, worked out from what is stored (SPEC §8). */
internal class DashboardData(
    val stats: List<WorkoutStats>,
    val work: List<MuscleWork>,
    val records: List<RecordEntry>,
    val metrics: List<BodyMetric>,
)

internal fun dashboard(
    data: DashboardData,
    settings: UserSettings,
    today: LocalDate,
    firstDay: DayOfWeek,
    zone: ZoneId,
): ProgressUiState.Ready {
    val target = settings.weeklySetTarget
    val sets = muscleSets(data.work, settings.secondaryMuscleSets)
    val weeks = weeklyTotals(data.stats, CHART_WEEKS, today, firstDay, zone)
    return ProgressUiState.Ready(
        hasWorkouts = data.stats.isNotEmpty(),
        unit = settings.weightUnit,
        streakWeeks = weeklyStreak(data.stats.map { it.day(zone) }, today, firstDay),
        thisWeek = weeks.last(),
        weeks = weeks,
        calendar = calendar(data.stats, today, firstDay, zone),
        muscles = muscleRows(sets, target),
        target = target,
        levels = sets.mapValues { (_, count) -> (count / target.first).coerceIn(0.0, 1.0).toFloat() },
        records = data.records,
        bodyWeight = data.metrics.firstOrNull { it.type == BodyMetricType.BODY_WEIGHT },
    )
}

private fun muscleRows(sets: Map<Muscle, Double>, target: IntRange): List<MuscleRow> =
    Muscle.entries.map { muscle -> (sets[muscle] ?: 0.0).let { MuscleRow(muscle, it, muscleLoad(it, target)) } }
        .sortedByDescending { it.sets }

private fun calendar(
    stats: List<WorkoutStats>,
    today: LocalDate,
    firstDay: DayOfWeek,
    zone: ZoneId,
): List<CalendarWeek> {
    val perDay = setsPerDay(stats, zone)
    val current = weekStart(today, firstDay)
    return (CALENDAR_WEEKS - 1 downTo 0).map { back ->
        val start = current.minusWeeks(back.toLong())
        CalendarWeek(
            start = start,
            sets = (0 until DAYS_PER_WEEK).map { day ->
                val date = start.plusDays(day.toLong())
                if (date.isAfter(today)) null else perDay[date] ?: 0
            },
        )
    }
}
