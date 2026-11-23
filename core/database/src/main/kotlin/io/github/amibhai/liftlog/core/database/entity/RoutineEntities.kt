package io.github.amibhai.liftlog.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import io.github.amibhai.liftlog.core.domain.model.ProgressionRuleType

@Entity(tableName = "routine", indices = [Index(value = ["position"])])
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String?,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

/** One planned exercise of a routine (SPEC §7, §10). */
@Entity(
    tableName = "routine_exercise",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exercise_id"]),
    ],
    indices = [Index(value = ["routine_id", "position"]), Index(value = ["exercise_id"])],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val position: Int,
    /** Exercises sharing a group number form a superset; null when not grouped. */
    @ColumnInfo(name = "superset_group") val supersetGroup: Int?,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "rep_min") val repMin: Int?,
    @ColumnInfo(name = "rep_max") val repMax: Int?,
    @ColumnInfo(name = "target_rpe") val targetRpe: Double?,
    @ColumnInfo(name = "target_rir") val targetRir: Int?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
    @ColumnInfo(name = "progression_rule") val progressionRule: ProgressionRuleType,
    /** Rule-specific parameters as JSON (increment, deload %, training max, ...). */
    @ColumnInfo(name = "rule_params") val ruleParams: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

/** A program is an ordered rotation of routines; Home suggests the next day (SPEC §7). */
@Entity(tableName = "program")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    /** Key of the built-in template it was created from (e.g. "ppl", "531"); null if custom. */
    @ColumnInfo(name = "template_key") val templateKey: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "program_day",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["program_id", "position"]), Index(value = ["routine_id"])],
)
data class ProgramDayEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "program_id") val programId: String,
    val position: Int,
    val name: String,
    @ColumnInfo(name = "routine_id") val routineId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)
