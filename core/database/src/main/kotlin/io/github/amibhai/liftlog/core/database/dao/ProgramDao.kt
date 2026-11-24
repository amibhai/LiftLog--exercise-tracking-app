package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Transaction
import androidx.room3.Upsert
import io.github.amibhai.liftlog.core.database.entity.ProgramDayEntity
import io.github.amibhai.liftlog.core.database.entity.ProgramEntity
import io.github.amibhai.liftlog.core.database.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

/** Whole programs. Relations include soft-deleted rows in no particular order; the mapping sorts that out. */
@Dao
interface ProgramDao {

    @Transaction
    @Query("SELECT * FROM program WHERE deleted_at IS NULL ORDER BY is_active DESC, created_at")
    fun observeAll(): Flow<List<ProgramGraph>>

    @Transaction
    @Query("SELECT * FROM program WHERE id = :id AND deleted_at IS NULL")
    fun observe(id: String): Flow<ProgramGraph?>

    @Transaction
    @Query("SELECT * FROM program WHERE id = :id AND deleted_at IS NULL")
    suspend fun load(id: String): ProgramGraph?

    /** The day that was done last, of the program that is followed. */
    @Query(
        """
        SELECT w.program_day_id FROM workout w
        JOIN program_day d ON d.id = w.program_day_id
        JOIN program p ON p.id = d.program_id
        WHERE p.is_active = 1 AND p.deleted_at IS NULL AND w.is_in_progress = 0 AND w.deleted_at IS NULL
        ORDER BY w.started_at DESC LIMIT 1
        """,
    )
    fun observeLastDay(): Flow<String?>

    @Upsert
    suspend fun upsertProgram(program: ProgramEntity)

    @Upsert
    suspend fun upsertDays(days: List<ProgramDayEntity>)

    @Query("UPDATE program_day SET deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun softDeleteDays(ids: List<String>, now: Long)

    /** Null follows no program. */
    @Query(
        "UPDATE program SET is_active = CASE WHEN id = :id THEN 1 ELSE 0 END, updated_at = :now " +
            "WHERE is_active = 1 OR id = :id",
    )
    suspend fun activate(id: String?, now: Long)

    @Query("UPDATE program SET deleted_at = :now, is_active = 0, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

data class ProgramGraph(
    @Embedded val program: ProgramEntity,
    @Relation(entity = ProgramDayEntity::class, parentColumns = ["id"], entityColumns = ["program_id"])
    val days: List<ProgramDayGraph>,
)

data class ProgramDayGraph(
    @Embedded val day: ProgramDayEntity,
    @Relation(parentColumns = ["routine_id"], entityColumns = ["id"])
    val routine: RoutineEntity?,
)
