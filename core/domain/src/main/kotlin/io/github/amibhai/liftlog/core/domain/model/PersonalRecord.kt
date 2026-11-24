package io.github.amibhai.liftlog.core.domain.model

/** Kinds of personal record (SPEC §8). Names are stored in the database. */
enum class RecordType {
    HEAVIEST_WEIGHT,
    BEST_E1RM,

    /** Most reps done at one weight; every weight has its own record. */
    REPS_AT_WEIGHT,
    BEST_SET_VOLUME,
    BEST_SESSION_VOLUME,
    MOST_REPS,

    /** For assisted exercises, where less assistance is better. */
    LEAST_ASSISTANCE,
    LONGEST_DURATION,
    LONGEST_DISTANCE,
}

/**
 * A record at the moment it was broken. [value] is in the record's own unit: kg, reps, seconds or meters.
 * [weightKg] and [reps] describe the set, when one set broke it.
 */
data class PersonalRecord(
    val exerciseId: ExerciseId,
    val type: RecordType,
    val value: Double,
    val weightKg: Double?,
    val reps: Int?,
    val workoutId: WorkoutId,
    val setId: WorkoutSetId?,
    val achievedAt: Long,
)

/** A record with what its exercise is called, for lists across exercises. */
data class RecordEntry(val record: PersonalRecord, val exerciseName: String, val exerciseType: ExerciseType)
