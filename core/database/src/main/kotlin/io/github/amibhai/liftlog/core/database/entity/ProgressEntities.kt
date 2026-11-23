package io.github.amibhai.liftlog.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Derived cache of personal records (SPEC §8); can always be rebuilt from workout history. */
@Entity(
    tableName = "personal_record",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_set_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["exercise_id", "record_type"]),
        Index(value = ["workout_id"]),
        Index(value = ["workout_set_id"]),
    ],
)
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    /** Record kind (heaviest weight, best e1RM, ...); stored by name, defined in Phase 2. */
    @ColumnInfo(name = "record_type") val recordType: String,
    val value: Double,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    val reps: Int?,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    @ColumnInfo(name = "workout_set_id") val workoutSetId: String?,
    @ColumnInfo(name = "achieved_at") val achievedAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

/** Body weight or a measurement (SPEC §8). Canonical units: kg and cm. */
@Entity(tableName = "body_metric", indices = [Index(value = ["metric_type", "measured_at"])])
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    /** BODY_WEIGHT, WAIST, CHEST, ...; stored by name, defined in Phase 2. */
    @ColumnInfo(name = "metric_type") val metricType: String,
    val value: Double,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

/** A progress photo kept in private app storage (SPEC §8); only the file name is stored here. */
@Entity(tableName = "progress_photo", indices = [Index(value = ["taken_at"])])
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "taken_at") val takenAt: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    val pose: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)
