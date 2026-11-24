package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Transaction
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

    /**
     * FTS prefix search. [match] comes from `FtsQuery.prefix` and [query] from `SearchText.normalize`.
     *
     * Best first (SPEC §4): the exact name; a whole alias or search term ("ohp"); a prefix of the name or
     * of an alias; any other match. Ties go to popularity, then name. Newlines mark where an alias or term
     * starts and ends.
     */
    @Query(
        """
        SELECT e.id, e.name, e.type, e.equipment, e.is_custom,
            (SELECT m.muscle FROM exercise_muscle m
                WHERE m.exercise_id = e.id AND m.is_primary = 1 AND m.deleted_at IS NULL
                ORDER BY m.rowid LIMIT 1) AS primary_muscle,
            (SELECT COALESCE(i.thumbnail_url, i.url) FROM exercise_image i
                WHERE i.exercise_id = e.id AND i.deleted_at IS NULL
                ORDER BY i.position LIMIT 1) AS thumbnail_url
        FROM exercise e
        JOIN exercise_fts ON exercise_fts.rowid = e.rowid
        WHERE exercise_fts MATCH :match AND e.deleted_at IS NULL AND e.is_archived = 0
        ORDER BY
            CASE
                WHEN lower(replace(e.name, '-', ' ')) = :query THEN 0
                WHEN instr(
                    char(10) || lower(replace(e.aliases, '-', ' ')) || char(10) || e.search_terms || char(10),
                    char(10) || :query || char(10)
                ) > 0 THEN 1
                WHEN instr(
                    char(10) || lower(replace(e.name || char(10) || e.aliases, '-', ' ')),
                    char(10) || :query
                ) > 0 THEN 2
                ELSE 3
            END,
            e.popularity_rank IS NULL, e.popularity_rank, e.name COLLATE NOCASE
        LIMIT :limit
        """,
    )
    fun search(match: String, query: String, limit: Int): Flow<List<ExerciseSearchRow>>

    @Transaction
    @Query("SELECT * FROM exercise WHERE id = :id AND deleted_at IS NULL")
    fun observeWithDetails(id: String): Flow<ExerciseWithDetails?>

    @Query("SELECT * FROM library_source WHERE deleted_at IS NULL ORDER BY exercise_count DESC, name")
    fun observeSources(): Flow<List<LibrarySourceEntity>>

    /** Every non-deleted exercise; for exports and library updates, not for UI lists. */
    @Query("SELECT * FROM exercise WHERE deleted_at IS NULL ORDER BY name")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise_muscle WHERE deleted_at IS NULL")
    suspend fun getAllMuscles(): List<ExerciseMuscleEntity>

    @Insert
    suspend fun insert(exercise: ExerciseEntity)

    @Insert
    suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Insert
    suspend fun insertImages(images: List<ExerciseImageEntity>)

    @Insert
    suspend fun insertSources(sources: List<LibrarySourceEntity>)

    @Query("UPDATE exercise SET name = :name, updated_at = :now, user_modified_at = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long)

    @Query("UPDATE exercise SET is_archived = :archived, updated_at = :now, user_modified_at = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("UPDATE exercise SET deleted_at = :now, updated_at = :now WHERE id = :id AND is_custom = 1")
    suspend fun softDeleteCustom(id: String, now: Long)
}

data class ExerciseSearchRow(
    val id: String,
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "primary_muscle") val primaryMuscle: Muscle?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
)

data class ExerciseWithDetails(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["exercise_id"])
    val muscles: List<ExerciseMuscleEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["exercise_id"])
    val images: List<ExerciseImageEntity>,
)
