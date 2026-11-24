package io.github.amibhai.liftlog.core.domain.model

import io.github.amibhai.liftlog.core.domain.units.Weight

/** A rest countdown, stored as an absolute end time so it survives process death (SPEC §6). */
data class RestTimer(val endsAt: Long, val totalSeconds: Int)

/** The parts of an exercise that logging needs. */
data class ExerciseInfo(
    val id: ExerciseId,
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    /** Pinned note, shown every time the exercise is logged (SPEC §5). */
    val stickyNote: String? = null,
    val defaults: ExerciseDefaults = ExerciseDefaults(),
)

/** Per-exercise settings; null falls back to the app settings (SPEC §2). */
data class ExerciseDefaults(
    val restSeconds: Int? = null,
    val weightStepKg: Double? = null,
    val weightStepLb: Double? = null,
    val barWeight: Weight? = null,
)

data class WorkoutSet(
    val id: WorkoutSetId,
    val type: SetType = SetType.WORKING,
    /** What was typed or logged. */
    val entered: SetValues = SetValues.EMPTY,
    /** The pre-filled ghost values (SPEC §5). */
    val target: SetValues = SetValues.EMPTY,
    val rpe: Double? = null,
    val rir: Int? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
) {
    /** What the row shows and what ✓ logs: typed values win over ghost values. */
    val shown: SetValues get() = entered.orElse(target)
}

data class WorkoutExercise(
    val id: WorkoutExerciseId,
    val exercise: ExerciseInfo,
    /** Neighbouring exercises with the same number form a superset. */
    val supersetGroup: Int? = null,
    val notes: String? = null,
    val restSeconds: Int? = null,
    val routineExerciseId: RoutineExerciseId? = null,
    val sets: List<WorkoutSet> = emptyList(),
)

/** A logged or in-progress workout. */
data class Workout(
    val id: WorkoutId,
    val name: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val routineId: RoutineId? = null,
    /** The day of a program that this workout counts for. */
    val programDayId: ProgramDayId? = null,
    val notes: String? = null,
    val isInProgress: Boolean = true,
    val restTimer: RestTimer? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
)

/** A completed set of an earlier session. */
data class LoggedSet(
    val type: SetType,
    val values: SetValues,
    val rpe: Double? = null,
    val rir: Int? = null,
    /** Null for a set that is not stored yet. */
    val id: WorkoutSetId? = null,
)

/** One exercise's completed sets in one past workout. */
data class ExerciseSession(val workoutId: WorkoutId, val at: Long, val sets: List<LoggedSet>)

/** A workout that contained an exercise; the raw data behind frecency and co-occurrence (SPEC §4). */
data class ExerciseUsage(val exerciseId: ExerciseId, val workoutId: WorkoutId, val at: Long)
