package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Transaction
import androidx.room3.Upsert
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.RoutineEntity
import io.github.amibhai.liftlog.core.database.entity.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

/** Whole routines. Relations include soft-deleted rows in no particular order; the mapping sorts that out. */
@Dao
interface RoutineDao {

    @Transaction
    @Query("SELECT * FROM routine WHERE deleted_at IS NULL ORDER BY position, created_at")
    fun observeAll(): Flow<List<RoutineGraph>>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id AND deleted_at IS NULL")
    fun observe(id: String): Flow<RoutineGraph?>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id AND deleted_at IS NULL")
    suspend fun load(id: String): RoutineGraph?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM routine WHERE deleted_at IS NULL")
    suspend fun nextPosition(): Int

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertExercises(exercises: List<RoutineExerciseEntity>)

    @Query("UPDATE routine_exercise SET deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun softDeleteExercises(ids: List<String>, now: Long)

    /** [deletedAt] null restores the routine. */
    @Query("UPDATE routine SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setDeleted(id: String, deletedAt: Long?, now: Long)

    @Query("UPDATE routine SET position = :position, updated_at = :now WHERE id = :id")
    suspend fun setPosition(id: String, position: Int, now: Long)
}

data class RoutineGraph(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineExerciseEntity::class, parentColumns = ["id"], entityColumns = ["routine_id"])
    val exercises: List<RoutineExerciseGraph>,
)

data class RoutineExerciseGraph(
    @Embedded val routineExercise: RoutineExerciseEntity,
    @Relation(entity = ExerciseEntity::class, parentColumns = ["exercise_id"], entityColumns = ["id"])
    val exercise: ExerciseInfoRow,
)
