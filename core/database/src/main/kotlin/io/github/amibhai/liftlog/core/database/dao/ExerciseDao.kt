package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseImageEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.database.entity.LibrarySourceEntity
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT COUNT(*) FROM exercise WHERE deleted_at IS NULL AND is_archived = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM exercise WHERE deleted_at IS NULL AND is_archived = 0 AND is_custom = 1")
    fun observeCustomCount(): Flow<Int>

    /** FTS prefix search (SPEC §4), unranked: the domain ranks. [match] comes from `FtsQuery.prefix`. */
    @Query(
        """
        SELECT e.id FROM exercise e JOIN exercise_fts ON exercise_fts.rowid = e.rowid
        WHERE exercise_fts MATCH :match AND e.deleted_at IS NULL AND e.is_archived = 0
        """,
    )
    suspend fun matchingIds(match: String): List<String>

    /** Every visible exercise, without the long texts. */
    @Query(
        """
        SELECT id, name, aliases, search_terms, type, equipment, popularity_rank, is_custom, is_favorite
        FROM exercise WHERE deleted_at IS NULL AND is_archived = 0
        """,
    )
    fun observeCatalog(): Flow<List<CatalogRow>>

    @Query(
        """
        SELECT exercise_id, muscle FROM exercise_muscle
        WHERE is_primary = 1 AND deleted_at IS NULL ORDER BY exercise_id, rowid
        """,
    )
    fun observePrimaryMuscles(): Flow<List<PrimaryMuscleRow>>

    /** The first image of each exercise (bare columns next to MIN() come from that row). */
    @Query(
        """
        SELECT exercise_id, COALESCE(thumbnail_url, url) AS url, MIN(position) AS position
        FROM exercise_image WHERE deleted_at IS NULL GROUP BY exercise_id
        """,
    )
    fun observeThumbnails(): Flow<List<ThumbnailRow>>

    @Query(
        """
        SELECT id, name, type, equipment, notes, default_rest_s, weight_step_kg, weight_step_lb, bar_weight_kg
        FROM exercise WHERE id IN (:ids) AND deleted_at IS NULL
        """,
    )
    suspend fun infos(ids: List<String>): List<ExerciseInfoRow>

    @Transaction
    @Query("SELECT * FROM exercise WHERE id = :id AND deleted_at IS NULL")
    fun observeWithDetails(id: String): Flow<ExerciseWithDetails?>

    @Query("SELECT * FROM library_source WHERE deleted_at IS NULL ORDER BY exercise_count DESC, name")
    fun observeSources(): Flow<List<LibrarySourceEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id AND deleted_at IS NULL")
    suspend fun get(id: String): ExerciseEntity?

    /** Every non-deleted exercise; for exports and library updates, not for UI lists. */
    @Query("SELECT * FROM exercise WHERE deleted_at IS NULL ORDER BY name")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise_muscle WHERE deleted_at IS NULL")
    suspend fun getAllMuscles(): List<ExerciseMuscleEntity>

    @Insert
    suspend fun insert(exercise: ExerciseEntity)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    /** Including soft-deleted rows, which are revived when a muscle comes back. */
    @Query("SELECT * FROM exercise_muscle WHERE exercise_id = :id")
    suspend fun musclesOf(id: String): List<ExerciseMuscleEntity>

    @Upsert
    suspend fun upsertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Insert
    suspend fun insertImages(images: List<ExerciseImageEntity>)

    @Insert
    suspend fun insertSources(sources: List<LibrarySourceEntity>)

    @Query("UPDATE exercise SET is_archived = :archived, updated_at = :now, user_modified_at = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("UPDATE exercise SET is_favorite = :favorite, updated_at = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE exercise SET notes = :notes, updated_at = :now WHERE id = :id")
    suspend fun setNotes(id: String, notes: String?, now: Long)

    @Query(
        """
        UPDATE exercise SET default_rest_s = :restSeconds, weight_step_kg = :stepKg, weight_step_lb = :stepLb,
            bar_weight_kg = :barKg, updated_at = :now
        WHERE id = :id
        """,
    )
    suspend fun setDefaults(id: String, restSeconds: Int?, stepKg: Double?, stepLb: Double?, barKg: Double?, now: Long)

    @Query("UPDATE exercise SET deleted_at = :now, updated_at = :now WHERE id = :id AND is_custom = 1")
    suspend fun softDeleteCustom(id: String, now: Long)
}

data class CatalogRow(
    val id: String,
    val name: String,
    val aliases: String,
    @ColumnInfo(name = "search_terms") val searchTerms: String,
    val type: ExerciseType,
    val equipment: Equipment,
    @ColumnInfo(name = "popularity_rank") val popularityRank: Int?,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
)

data class PrimaryMuscleRow(@ColumnInfo(name = "exercise_id") val exerciseId: String, val muscle: Muscle)

data class ThumbnailRow(@ColumnInfo(name = "exercise_id") val exerciseId: String, val url: String, val position: Int)

data class ExerciseWithDetails(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["exercise_id"])
    val muscles: List<ExerciseMuscleEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["exercise_id"])
    val images: List<ExerciseImageEntity>,
)
