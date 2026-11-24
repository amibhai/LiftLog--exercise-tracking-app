package io.github.amibhai.liftlog.core.domain.progress

import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import java.util.concurrent.TimeUnit

/** The rep-max table covers one to twelve reps (SPEC §8). */
const val REP_MAX_LIMIT = 12

private const val DAYS_IN_MONTH = 30
private const val DAYS_IN_QUARTER = 91
private const val DAYS_IN_HALF_YEAR = 182
private const val DAYS_IN_YEAR = 365

/** One session of an exercise, as its charts show it. Only the sets that count are looked at. */
data class SessionProgress(
    val workoutId: WorkoutId,
    val at: Long,
    val bestE1rm: Double? = null,
    val topWeight: Double? = null,
    val volume: Double = 0.0,
    val bestReps: Int? = null,
    val totalReps: Int = 0,
    val bestDuration: Int? = null,
    val bestDistance: Double? = null,
)

/** The sessions of an exercise, oldest first, ready for the charts. */
fun sessionProgress(type: ExerciseType, sessions: List<ExerciseSession>, formula: E1rmFormula): List<SessionProgress> =
    sessions.sortedBy { it.at }.map { session ->
        val sets = session.countingSets
        val values = sets.map { it.values }
        SessionProgress(
            workoutId = session.workoutId,
            at = session.at,
            bestE1rm = values.mapNotNull { v ->
                v.weight?.let {
                    estimateOneRepMax(
                        it.kg,
                        v.reps ?: 0,
                        formula,
                    )
                }
            }.maxOrNull(),
            topWeight = values.filter { (it.reps ?: 0) > 0 || (it.distanceMeters ?: 0.0) > 0 }
                .mapNotNull { it.weight?.kg }.filter { it > 0 }.maxOrNull(),
            volume = sessionVolume(type, sets),
            bestReps = values.mapNotNull { it.reps }.filter { it > 0 }.maxOrNull(),
            totalReps = values.sumOf { it.reps ?: 0 },
            bestDuration = values.mapNotNull { it.durationSeconds }.filter { it > 0 }.maxOrNull(),
            bestDistance = values.mapNotNull { it.distanceMeters }.filter { it > 0 }.maxOrNull(),
        )
    }

/** A row of the rep-max table: the heaviest weight ever done for at least [reps] reps. */
data class RepMax(val reps: Int, val weightKg: Double, val doneReps: Int, val at: Long)

/** The rep-max table; a rep count nobody ever did is left out. */
fun repMaxes(sessions: List<ExerciseSession>): List<RepMax> {
    val done = sessions.flatMap { session -> session.countingSets.map { session.at to it } }
        .filter { (_, set) -> (set.values.weight?.kg ?: 0.0) > 0 && (set.values.reps ?: 0) > 0 }
    return (1..REP_MAX_LIMIT).mapNotNull { reps ->
        done.filter { (_, set) -> (set.values.reps ?: 0) >= reps }
            .maxWithOrNull(compareBy<Pair<Long, LoggedSet>> { it.second.values.weight?.kg ?: 0.0 }.thenBy { it.first })
            ?.let { (at, set) -> RepMax(reps, set.values.weight?.kg ?: 0.0, set.values.reps ?: 0, at) }
    }
}

/** What an exercise's chart can show. */
enum class ProgressMetric { E1RM, TOP_WEIGHT, VOLUME, REPS, DURATION, DISTANCE }

/** The metrics that make sense for this kind of exercise; the first one is shown first. */
fun ExerciseType.progressMetrics(): List<ProgressMetric> = when (weightMeaning) {
    WeightMeaning.LOAD -> if (this == ExerciseType.WEIGHT_DISTANCE) {
        listOf(ProgressMetric.TOP_WEIGHT, ProgressMetric.DISTANCE)
    } else {
        listOf(ProgressMetric.E1RM, ProgressMetric.TOP_WEIGHT, ProgressMetric.VOLUME, ProgressMetric.REPS)
    }

    WeightMeaning.ADDED -> listOf(ProgressMetric.TOP_WEIGHT, ProgressMetric.REPS, ProgressMetric.VOLUME)

    WeightMeaning.ASSISTANCE -> listOf(ProgressMetric.REPS, ProgressMetric.TOP_WEIGHT)

    WeightMeaning.NONE -> when (this) {
        ExerciseType.REPS_ONLY -> listOf(ProgressMetric.REPS)
        ExerciseType.DISTANCE_DURATION -> listOf(ProgressMetric.DISTANCE, ProgressMetric.DURATION)
        else -> listOf(ProgressMetric.DURATION)
    }
}

/** The point of [metric] for this session; null when it has none. */
fun SessionProgress.value(metric: ProgressMetric): Double? = when (metric) {
    ProgressMetric.E1RM -> bestE1rm
    ProgressMetric.TOP_WEIGHT -> topWeight
    ProgressMetric.VOLUME -> volume.takeIf { it > 0 }
    ProgressMetric.REPS -> bestReps?.toDouble()
    ProgressMetric.DURATION -> bestDuration?.toDouble()
    ProgressMetric.DISTANCE -> bestDistance
}

/** The time span a chart covers (SPEC §8). */
enum class ProgressRange(val days: Int?) {
    MONTH(DAYS_IN_MONTH),
    QUARTER(DAYS_IN_QUARTER),
    HALF_YEAR(DAYS_IN_HALF_YEAR),
    YEAR(DAYS_IN_YEAR),
    ALL(null),
}

/** The items of the last part of [range] before [now]. */
fun <T> List<T>.within(range: ProgressRange, now: Long, at: (T) -> Long): List<T> =
    range.days?.let { days -> now - TimeUnit.DAYS.toMillis(days.toLong()) }
        ?.let { since -> filter { at(it) >= since } }
        ?: this
