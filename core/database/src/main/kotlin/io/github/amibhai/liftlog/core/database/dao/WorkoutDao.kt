package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Transaction
import androidx.room3.Upsert
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutSetEntity
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import kotlinx.coroutines.flow.Flow

/** Whole workouts. Relations include soft-deleted rows in no particular order; the mapping sorts that out. */
@Dao
interface WorkoutDao {

    @Transaction
    @Query(
        "SELECT * FROM workout WHERE is_in_progress = 1 AND deleted_at IS NULL ORDER BY started_at DESC LIMIT 1",
    )
    fun observeActive(): Flow<WorkoutGraph?>

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :id AND deleted_at IS NULL")
    fun observe(id: String): Flow<WorkoutGraph?>

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :id AND deleted_at IS NULL")
    suspend fun load(id: String): WorkoutGraph?

    @Transaction
    @Query(
        "SELECT * FROM workout WHERE is_in_progress = 0 AND deleted_at IS NULL ORDER BY started_at DESC LIMIT :limit",
    )
    fun observeFinished(limit: Int): Flow<List<WorkoutGraph>>

    @Query("SELECT COUNT(*) FROM workout WHERE is_in_progress = 0 AND deleted_at IS NULL")
    fun observeFinishedCount(): Flow<Int>

    /** Including deleted ones. */
    @Query("SELECT id FROM workout WHERE id IN (:ids)")
    suspend fun knownIds(ids: List<String>): List<String>

    @Upsert
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Upsert
    suspend fun upsertExercises(exercises: List<WorkoutExerciseEntity>)

    @Upsert
    suspend fun upsertSets(sets: List<WorkoutSetEntity>)

    @Query("UPDATE workout_exercise SET deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun softDeleteExercises(ids: List<String>, now: Long)

    @Query("UPDATE workout_set SET deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun softDeleteSets(ids: List<String>, now: Long)

    /** [deletedAt] null restores the workout. */
    @Query("UPDATE workout SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setDeleted(id: String, deletedAt: Long?, now: Long)
}

data class WorkoutGraph(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumns = ["id"], entityColumns = ["workout_id"])
    val exercises: List<WorkoutExerciseGraph>,
)

data class WorkoutExerciseGraph(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(entity = ExerciseEntity::class, parentColumns = ["exercise_id"], entityColumns = ["id"])
    val exercise: ExerciseInfoRow,
    @Relation(parentColumns = ["id"], entityColumns = ["workout_exercise_id"])
    val sets: List<WorkoutSetEntity>,
)

/** The columns of an exercise that logging needs. */
data class ExerciseInfoRow(
    val id: String,
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val notes: String?,
    @ColumnInfo(name = "default_rest_s") val defaultRestSeconds: Int?,
    @ColumnInfo(name = "weight_step_kg") val weightStepKg: Double?,
    @ColumnInfo(name = "weight_step_lb") val weightStepLb: Double?,
    @ColumnInfo(name = "bar_weight_kg") val barWeightKg: Double?,
)
