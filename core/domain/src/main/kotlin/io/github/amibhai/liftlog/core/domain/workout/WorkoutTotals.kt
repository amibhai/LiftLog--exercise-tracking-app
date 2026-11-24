package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.units.Weight

private const val MILLIS_PER_SECOND = 1_000L

/** The header of a workout: volume lifted and sets done (SPEC §5). */
data class WorkoutTotals(val volume: Weight, val completedSets: Int, val totalSets: Int)

fun Workout.totals(): WorkoutTotals {
    val done = exercises.flatMap { exercise ->
        exercise.sets.filter(WorkoutSet::isCompleted).map { exercise.exercise.type to it }
    }
    return WorkoutTotals(
        volume = done.fold(Weight.ZERO) { sum, (type, set) -> sum + setVolume(type, set) },
        completedSets = done.size,
        totalSets = exercises.sumOf { it.sets.size },
    )
}

/** Seconds from the start to the end, or to [now] while in progress. */
fun Workout.durationSeconds(now: Long): Long = ((endedAt ?: now) - startedAt).coerceAtLeast(0) / MILLIS_PER_SECOND

/** The number shown on each working set; other set types show a letter instead. */
fun workingNumbers(sets: List<WorkoutSet>): List<Int?> {
    var number = 0
    return sets.map { if (it.type == SetType.WORKING) ++number else null }
}

/** A letter for each superset, in the order they appear; null outside supersets. */
fun <T> supersetLetters(items: List<T>, groupOf: (T) -> Int?): List<Char?> {
    val letters = mutableMapOf<Int, Char>()
    return items.map { item -> groupOf(item)?.let { group -> letters.getOrPut(group) { 'A' + letters.size } } }
}

/** The best set to sum up a session: the heaviest, then the one with most reps (or distance, or time). */
fun topSet(sets: List<LoggedSet>, type: ExerciseType = ExerciseType.WEIGHT_REPS): LoggedSet? {
    val candidates = sets.filter { it.type.countsTowardRecords }.ifEmpty { sets }
    // Less assistance is the better set.
    val sign = if (type.weightMeaning == WeightMeaning.ASSISTANCE) -1.0 else 1.0
    return candidates.maxWithOrNull(
        compareBy<LoggedSet> { (it.values.weight?.kg ?: 0.0) * sign }
            .thenBy { it.values.reps ?: 0 }
            .thenBy { it.values.distanceMeters ?: 0.0 }
            .thenBy { it.values.durationSeconds ?: 0 },
    )
}

/** Warm-ups never count (SPEC §5). Body weight is unknown until Phase 2, so only external load counts. */
private fun setVolume(type: ExerciseType, set: WorkoutSet): Weight {
    val lifted = type.weightMeaning == WeightMeaning.LOAD || type.weightMeaning == WeightMeaning.ADDED
    val kg = set.entered.weight?.kg ?: 0.0
    return if (lifted && set.type.countsTowardRecords) Weight(kg * (set.entered.reps ?: 0)) else Weight.ZERO
}
