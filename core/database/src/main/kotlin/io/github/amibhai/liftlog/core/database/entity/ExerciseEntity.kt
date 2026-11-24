package io.github.amibhai.liftlog.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Mechanic
import kotlinx.serialization.Serializable

/**
 * A built-in (from the prepopulated asset) or custom exercise (SPEC §3).
 *
 * Built-in rows are never deleted, only archived. Library updates rewrite built-in rows the user has
 * not edited ([userModifiedAt] is null). Nullable per-exercise settings fall back to UserSettings.
 */
@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["source_ref"], unique = true),
        Index(value = ["equipment"]),
        Index(value = ["popularity_rank"]),
    ],
)
@Serializable
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Display aliases, one per line. */
    val aliases: String,
    /** Hidden search terms (abbreviations, spellings, equipment and muscle words), normalized, one per line. */
    @ColumnInfo(name = "search_terms") val searchTerms: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val category: ExerciseCategory,
    val mechanic: Mechanic?,
    /** Instruction steps, one per line. */
    val instructions: String,
    @ColumnInfo(name = "is_unilateral") val isUnilateral: Boolean,
    @ColumnInfo(name = "default_rest_s") val defaultRestSeconds: Int?,
    /** −/+ step used while the app unit is kg, in kg. */
    @ColumnInfo(name = "weight_step_kg") val weightStepKg: Double?,
    /** −/+ step used while the app unit is lb, in lb (plate systems differ, so it is not converted). */
    @ColumnInfo(name = "weight_step_lb") val weightStepLb: Double?,
    @ColumnInfo(name = "bar_weight_kg") val barWeightKg: Double?,
    /** Sticky note shown every time the exercise is logged. */
    val notes: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    /** 1 = most popular; null for unranked. Drives the starter set and ties for new users. */
    @ColumnInfo(name = "popularity_rank") val popularityRank: Int?,
    /** "fedb:<id>" or "wger:<uuid>" for built-ins; null for custom exercises. */
    @ColumnInfo(name = "source_ref") val sourceRef: String?,
    val license: String?,
    @ColumnInfo(name = "license_url") val licenseUrl: String?,
    @ColumnInfo(name = "license_author") val licenseAuthor: String?,
    @ColumnInfo(name = "library_version") val libraryVersion: Int?,
    @ColumnInfo(name = "user_modified_at") val userModifiedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)
