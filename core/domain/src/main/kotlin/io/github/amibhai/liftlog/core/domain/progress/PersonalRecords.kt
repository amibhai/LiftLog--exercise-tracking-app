package io.github.amibhai.liftlog.core.domain.progress

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.PersonalRecord
import io.github.amibhai.liftlog.core.domain.model.RecordType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import kotlin.math.roundToLong

/** Weights within this many kg are the same weight; far below the smallest plate. */
private const val WEIGHT_PRECISION = 0.01

/** Floating-point noise from unit conversion must not count as progress. */
private const val EPSILON = 1e-6

/** The records worth tracking for this kind of exercise. */
fun ExerciseType.recordTypes(): List<RecordType> = when (this) {
    ExerciseType.WEIGHT_REPS -> listOf(
        RecordType.HEAVIEST_WEIGHT,
        RecordType.BEST_E1RM,
        RecordType.REPS_AT_WEIGHT,
        RecordType.BEST_SET_VOLUME,
        RecordType.BEST_SESSION_VOLUME,
    )

    ExerciseType.BODYWEIGHT_ADDED ->
        listOf(RecordType.HEAVIEST_WEIGHT, RecordType.MOST_REPS, RecordType.REPS_AT_WEIGHT)

    ExerciseType.BODYWEIGHT_ASSISTED -> listOf(RecordType.LEAST_ASSISTANCE, RecordType.MOST_REPS)

    ExerciseType.REPS_ONLY -> listOf(RecordType.MOST_REPS)

    ExerciseType.DURATION -> listOf(RecordType.LONGEST_DURATION)

    ExerciseType.DISTANCE_DURATION -> listOf(RecordType.LONGEST_DISTANCE, RecordType.LONGEST_DURATION)

    ExerciseType.WEIGHT_DISTANCE -> listOf(RecordType.HEAVIEST_WEIGHT, RecordType.LONGEST_DISTANCE)
}

/** The best so far of every record of one exercise, against which new sets are measured. */
data class Bests(
    val scores: Map<RecordType, Double> = emptyMap(),
    /** Most reps at each weight. */
    val repsAtWeight: Map<Long, Int> = emptyMap(),
)

/** The bests after every session in [sessions], in any order. */
fun bestsOf(type: ExerciseType, sessions: List<ExerciseSession>, formula: E1rmFormula): Bests =
    sessions.sortedBy { it.at }.fold(Bests()) { bests, session -> bests.after(type, session, formula) }

/** Every record that a session broke, oldest first. A kind's first value is only a baseline. */
fun recordTimeline(
    exercise: ExerciseId,
    type: ExerciseType,
    sessions: List<ExerciseSession>,
    formula: E1rmFormula,
): List<PersonalRecord> {
    var bests = Bests()
    return sessions.sortedBy { it.at }.flatMap { session ->
        recordsBroken(exercise, type, session, bests, formula)
            .also { bests = bests.after(type, session, formula) }
    }
}

/** The records [session] breaks over [bests] from before it: one per kind, and per weight for rep records. */
fun recordsBroken(
    exercise: ExerciseId,
    type: ExerciseType,
    session: ExerciseSession,
    bests: Bests,
    formula: E1rmFormula,
): List<PersonalRecord> {
    val sets = session.countingSets
    val record = RecordFactory(exercise, session)
    return type.recordTypes().flatMap { kind ->
        when (kind) {
            RecordType.REPS_AT_WEIGHT -> repRecords(sets, bests).map { (set, reps) ->
                record.of(kind, reps.toDouble(), set)
            }

            RecordType.BEST_SESSION_VOLUME -> listOfNotNull(
                sessionVolume(type, sets).takeIf { bests.beats(kind, it) }?.let { record.of(kind, it, set = null) },
            )

            else -> listOfNotNull(
                sets.bestBy(kind, formula)?.takeIf { (_, score) -> bests.beats(kind, score) }
                    ?.let { (set, score) -> record.of(kind, kind.valueOf(score), set) },
            )
        }
    }
}

/** The records one set breaks, compared with [bests] from before its workout (SPEC §5: PR badges). */
fun recordsBrokenBy(type: ExerciseType, set: LoggedSet, bests: Bests, formula: E1rmFormula): Set<RecordType> =
    type.recordTypes().filter { kind ->
        set.type.countsTowardRecords && when (kind) {
            RecordType.REPS_AT_WEIGHT -> repRecords(listOf(set), bests).isNotEmpty()
            RecordType.BEST_SESSION_VOLUME -> false
            else -> set.values.score(kind, formula)?.let { bests.beats(kind, it) } ?: false
        }
    }.toSet()

/** The bests once [session] is over. */
fun Bests.after(type: ExerciseType, session: ExerciseSession, formula: E1rmFormula): Bests {
    val sets = session.countingSets
    val newScores = buildMap {
        putAll(scores)
        for (kind in type.recordTypes()) {
            val score = when (kind) {
                RecordType.BEST_SESSION_VOLUME -> sessionVolume(type, sets).takeIf { it > 0 }
                RecordType.REPS_AT_WEIGHT -> null
                else -> sets.bestBy(kind, formula)?.second
            }
            if (score != null && score > (scores[kind] ?: Double.NEGATIVE_INFINITY)) put(kind, score)
        }
    }
    val newReps = buildMap {
        putAll(repsAtWeight)
        for (set in sets) {
            val key = set.weightKey ?: continue
            val reps = set.values.reps ?: 0
            if (reps > (this[key] ?: 0)) put(key, reps)
        }
    }
    return Bests(newScores, newReps)
}

/** Volume of the sets that count: what is lifted, not what assists. */
fun sessionVolume(type: ExerciseType, sets: List<LoggedSet>): Double = when (type.weightMeaning) {
    WeightMeaning.LOAD, WeightMeaning.ADDED -> sets.sumOf { it.values.volume ?: 0.0 }
    WeightMeaning.NONE, WeightMeaning.ASSISTANCE -> 0.0
}

/** The sets that count toward records: neither warm-ups nor empty sets. */
val ExerciseSession.countingSets: List<LoggedSet> get() = sets.filter { it.type.countsTowardRecords }

private fun Bests.beats(kind: RecordType, score: Double): Boolean = scores[kind]?.let { score > it + EPSILON } ?: false

/** The set with the best score of a kind, with its score; the first one wins ties. */
private fun List<LoggedSet>.bestBy(kind: RecordType, formula: E1rmFormula): Pair<LoggedSet, Double>? =
    mapNotNull { set -> set.values.score(kind, formula)?.let { set to it } }.maxByOrNull { it.second }

/** The sets that did more reps than ever at their weight, with those reps. */
private fun repRecords(sets: List<LoggedSet>, bests: Bests): List<Pair<LoggedSet, Int>> = sets
    .mapNotNull { set -> set.weightKey?.let { key -> Triple(key, set, set.values.reps ?: 0) } }
    .groupBy { it.first }
    .mapNotNull { (key, group) ->
        val best = group.maxBy { it.third }
        val previous = bests.repsAtWeight[key]
        (best.second to best.third).takeIf { previous != null && best.third > previous }
    }

private val LoggedSet.weightKey: Long?
    get() = values.weight?.kg?.takeIf {
        it > 0 && (values.reps ?: 0) > 0
    }?.let { (it / WEIGHT_PRECISION).roundToLong() }

/** The best of a kind so far, for the record table (SPEC §8). */
data class BestMark(
    val type: RecordType,
    val value: Double,
    val weightKg: Double?,
    val reps: Int?,
    val at: Long,
    val workoutId: WorkoutId,
)

/** The standing bests of an exercise, except for the rep records of single weights. */
fun bestMarks(type: ExerciseType, sessions: List<ExerciseSession>, formula: E1rmFormula): List<BestMark> =
    (type.recordTypes() - RecordType.REPS_AT_WEIGHT).mapNotNull { kind ->
        if (kind == RecordType.BEST_SESSION_VOLUME) bestSession(type, sessions) else bestSet(kind, sessions, formula)
    }

private fun bestSet(kind: RecordType, sessions: List<ExerciseSession>, formula: E1rmFormula): BestMark? = sessions
    .flatMap { session -> session.countingSets.map { session to it } }
    .mapNotNull { (session, set) -> set.values.score(kind, formula)?.let { score -> Triple(session, set, score) } }
    .maxByOrNull { it.third }
    ?.let { (session, set, score) ->
        BestMark(
            type = kind,
            value = kind.valueOf(score),
            weightKg = set.values.weight?.kg,
            reps = set.values.reps,
            at = session.at,
            workoutId = session.workoutId,
        )
    }

private fun bestSession(type: ExerciseType, sessions: List<ExerciseSession>): BestMark? = sessions
    .map { it to sessionVolume(type, it.countingSets) }
    .filter { it.second > 0 }
    .maxByOrNull { it.second }
    ?.let { (session, volume) ->
        BestMark(RecordType.BEST_SESSION_VOLUME, volume, null, null, session.at, session.workoutId)
    }

/** What a set scores on a record; a higher score is better. Null when the record does not apply to it. */
internal fun SetValues.score(kind: RecordType, formula: E1rmFormula): Double? = when (kind) {
    RecordType.HEAVIEST_WEIGHT -> weight?.kg?.takeIf { it > 0 && isDone }
    RecordType.BEST_E1RM -> weight?.let { estimateOneRepMax(it.kg, reps ?: 0, formula) }
    RecordType.BEST_SET_VOLUME -> volume?.takeIf { it > 0 }
    RecordType.MOST_REPS -> reps?.takeIf { it > 0 }?.toDouble()
    RecordType.LEAST_ASSISTANCE -> weight?.takeIf { isDone }?.let { -it.kg }
    RecordType.LONGEST_DURATION -> durationSeconds?.takeIf { it > 0 }?.toDouble()
    RecordType.LONGEST_DISTANCE -> distanceMeters?.takeIf { it > 0 }
    RecordType.REPS_AT_WEIGHT, RecordType.BEST_SESSION_VOLUME -> null
}

/** The score as the record's own value: less assistance scores higher but is stored as kg of assistance. */
private fun RecordType.valueOf(score: Double): Double = if (this == RecordType.LEAST_ASSISTANCE) -score else score

/** A set without reps or distance was not done, whatever its weight says. */
private val SetValues.isDone: Boolean get() = (reps ?: 0) > 0 || (distanceMeters ?: 0.0) > 0.0

private val SetValues.volume: Double? get() = weight?.let { it.kg * (reps ?: 0) }

private class RecordFactory(private val exercise: ExerciseId, private val session: ExerciseSession) {
    fun of(kind: RecordType, value: Double, set: LoggedSet?) = PersonalRecord(
        exerciseId = exercise,
        type = kind,
        value = value,
        weightKg = set?.values?.weight?.kg,
        reps = set?.values?.reps,
        workoutId = session.workoutId,
        setId = set?.id,
        achievedAt = session.at,
    )
}
