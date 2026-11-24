package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.Query
import io.github.amibhai.liftlog.core.database.entity.PersonalRecordEntity
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.SetType
import kotlinx.coroutines.flow.Flow

/** The completed sets of finished workouts. */
private const val FINISHED_SETS = """
    FROM workout w
    JOIN workout_exercise we ON we.workout_id = w.id AND we.deleted_at IS NULL
    JOIN workout_set s ON s.workout_exercise_id = we.id AND s.is_completed = 1 AND s.deleted_at IS NULL
    WHERE w.is_in_progress = 0 AND w.deleted_at IS NULL
"""

private const val PROGRESS_SET_COLUMNS =
    "s.id AS set_id, we.exercise_id, we.workout_id, w.started_at AS at, " +
        "s.type, s.weight_kg, s.reps, s.duration_s, s.distance_m, s.rpe, s.rir"

private const val IN_ORDER = "ORDER BY w.started_at, w.id, we.position, s.position"

/** What finished workouts add up to (SPEC §8), and the cache of personal records. */
@Dao
interface ProgressDao {

    @Query("SELECT $PROGRESS_SET_COLUMNS $FINISHED_SETS AND we.exercise_id = :exerciseId $IN_ORDER")
    fun observeSets(exerciseId: String): Flow<List<ProgressSetRow>>

    @Query(
        "SELECT $PROGRESS_SET_COLUMNS $FINISHED_SETS AND we.exercise_id IN (:ids) AND w.started_at < :before $IN_ORDER",
    )
    suspend fun setsBefore(ids: List<String>, before: Long): List<ProgressSetRow>

    @Query("SELECT $PROGRESS_SET_COLUMNS $FINISHED_SETS AND we.exercise_id IN (:ids) $IN_ORDER")
    suspend fun setsOf(ids: List<String>): List<ProgressSetRow>

    /** Volume only counts loads that are lifted, and no warm-ups (SPEC §5). */
    @Query(
        """
        SELECT w.id, w.started_at, w.ended_at, COUNT(s.id) AS sets,
            COALESCE(SUM(
                CASE WHEN s.type <> 'WARMUP' AND e.type IN ('WEIGHT_REPS', 'BODYWEIGHT_ADDED')
                THEN COALESCE(s.weight_kg, 0) * COALESCE(s.reps, 0) ELSE 0 END
            ), 0) AS volume
        FROM workout w
        LEFT JOIN workout_exercise we ON we.workout_id = w.id AND we.deleted_at IS NULL
        LEFT JOIN exercise e ON e.id = we.exercise_id
        LEFT JOIN workout_set s ON s.workout_exercise_id = we.id AND s.is_completed = 1 AND s.deleted_at IS NULL
        WHERE w.is_in_progress = 0 AND w.deleted_at IS NULL
        GROUP BY w.id ORDER BY w.started_at
        """,
    )
    fun observeWorkoutStats(): Flow<List<WorkoutStatsRow>>

    @Query(
        "SELECT we.exercise_id, COUNT(s.id) AS sets $FINISHED_SETS AND s.type <> 'WARMUP' " +
            "AND w.started_at >= :from AND w.started_at < :to GROUP BY we.exercise_id",
    )
    fun observeExerciseSets(from: Long, to: Long): Flow<List<ExerciseSetsRow>>

    @Query(
        "SELECT we.exercise_id, COUNT(s.id) AS sets $FINISHED_SETS AND s.type <> 'WARMUP' " +
            "AND w.id = :workoutId GROUP BY we.exercise_id",
    )
    fun observeExerciseSets(workoutId: String): Flow<List<ExerciseSetsRow>>

    @Query(
        "SELECT exercise_id, muscle, is_primary FROM exercise_muscle " +
            "WHERE deleted_at IS NULL AND exercise_id IN (:exerciseIds)",
    )
    suspend fun muscles(exerciseIds: List<String>): List<ExerciseMuscleRow>

    /** Exercises with a finished set. */
    @Query("SELECT DISTINCT we.exercise_id $FINISHED_SETS")
    suspend fun trainedExercises(): List<String>

    @Query("SELECT id, type FROM exercise WHERE id IN (:ids)")
    suspend fun typesOf(ids: List<String>): List<ExerciseTypeRow>

    @Query("SELECT COUNT(*) FROM personal_record WHERE deleted_at IS NULL")
    suspend fun recordCount(): Int

    /** Records are a cache, so they are really deleted. */
    @Query("DELETE FROM personal_record WHERE exercise_id IN (:exerciseIds)")
    suspend fun deleteRecords(exerciseIds: List<String>)

    @Insert
    suspend fun insertRecords(records: List<PersonalRecordEntity>)

    @Query(
        "SELECT r.*, e.name AS exercise_name, e.type AS exercise_type FROM personal_record r " +
            "JOIN exercise e ON e.id = r.exercise_id WHERE r.deleted_at IS NULL " +
            "ORDER BY r.achieved_at DESC, r.exercise_id, r.id LIMIT :limit",
    )
    fun observeRecentRecords(limit: Int): Flow<List<RecordRow>>

    @Query(
        "SELECT r.*, e.name AS exercise_name, e.type AS exercise_type FROM personal_record r " +
            "JOIN exercise e ON e.id = r.exercise_id WHERE r.deleted_at IS NULL AND r.workout_id = :workoutId " +
            "ORDER BY e.name, r.record_type, r.id",
    )
    fun observeRecordsOf(workoutId: String): Flow<List<RecordRow>>
}

/** A completed set of a finished workout. */
data class ProgressSetRow(
    @ColumnInfo(name = "set_id") val setId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    val at: Long,
    val type: SetType,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    val reps: Int?,
    @ColumnInfo(name = "duration_s") val durationSeconds: Int?,
    @ColumnInfo(name = "distance_m") val distanceMeters: Double?,
    val rpe: Double?,
    val rir: Int?,
)

data class WorkoutStatsRow(
    val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    val sets: Int,
    val volume: Double,
)

data class ExerciseSetsRow(@ColumnInfo(name = "exercise_id") val exerciseId: String, val sets: Int)

data class ExerciseMuscleRow(
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val muscle: Muscle,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean,
)

data class ExerciseTypeRow(val id: String, val type: ExerciseType)

/** A record with the exercise it belongs to. */
data class RecordRow(
    @Embedded val record: PersonalRecordEntity,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "exercise_type") val exerciseType: ExerciseType,
)
