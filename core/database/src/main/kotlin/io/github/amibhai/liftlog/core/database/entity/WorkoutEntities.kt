package io.github.amibhai.liftlog.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import io.github.amibhai.liftlog.core.domain.model.SetType
import kotlinx.serialization.Serializable

/**
 * A logged or in-progress workout (SPEC §5). The in-progress workout is written on every change and
 * its rest timer is stored as an absolute end time, so both survive process death (SPEC §6).
 */
@Entity(
    tableName = "workout",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ProgramDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_day_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["started_at"]),
        Index(value = ["is_in_progress"]),
        Index(value = ["routine_id"]),
        Index(value = ["program_day_id"]),
    ],
)
@Serializable
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "routine_id") val routineId: String?,
    @ColumnInfo(name = "program_day_id") val programDayId: String?,
    val notes: String?,
    @ColumnInfo(name = "is_in_progress") val isInProgress: Boolean,
    /** Body weight at the time, for bodyweight-exercise volume. */
    @ColumnInfo(name = "bodyweight_kg") val bodyweightKg: Double?,
    @ColumnInfo(name = "rest_timer_ends_at") val restTimerEndsAt: Long?,
    @ColumnInfo(name = "rest_timer_total_s") val restTimerTotalSeconds: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exercise_id"]),
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_exercise_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["workout_id", "position"]),
        Index(value = ["exercise_id", "workout_id"]),
        Index(value = ["routine_exercise_id"]),
    ],
)
@Serializable
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val position: Int,
    @ColumnInfo(name = "superset_group") val supersetGroup: Int?,
    val notes: String?,
    /** The routine slot this came from, for progression suggestions. */
    @ColumnInfo(name = "routine_exercise_id") val routineExerciseId: String?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

/** One set. Weights are canonical kg (SPEC §2); target values are the pre-filled ghost values (SPEC §5). */
@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["workout_exercise_id", "position"]), Index(value = ["completed_at"])],
)
@Serializable
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_exercise_id") val workoutExerciseId: String,
    val position: Int,
    val type: SetType,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    val reps: Int?,
    @ColumnInfo(name = "duration_s") val durationSeconds: Int?,
    @ColumnInfo(name = "distance_m") val distanceMeters: Double?,
    val rpe: Double?,
    val rir: Int?,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double?,
    @ColumnInfo(name = "target_reps") val targetReps: Int?,
    @ColumnInfo(name = "target_duration_s") val targetDurationSeconds: Int?,
    @ColumnInfo(name = "target_distance_m") val targetDistanceMeters: Double?,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)
