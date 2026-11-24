package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query
import io.github.amibhai.liftlog.core.domain.model.SetType
import kotlinx.coroutines.flow.Flow

/** Joins the exercises of finished workouts that have at least one completed set. */
private const val DONE = """
    FROM workout_exercise we JOIN workout w ON w.id = we.workout_id
    WHERE w.is_in_progress = 0 AND w.deleted_at IS NULL AND we.deleted_at IS NULL
        AND EXISTS (
            SELECT 1 FROM workout_set done
            WHERE done.workout_exercise_id = we.id AND done.is_completed = 1 AND done.deleted_at IS NULL
        )
"""

private const val SET_COLUMNS = "s.type, s.weight_kg, s.reps, s.duration_s, s.distance_m, s.rpe, s.rir"

/** Bare columns next to MAX() come from the row with the maximum (documented SQLite behaviour). */
private const val LATEST = "SELECT we.id, we.exercise_id, we.workout_id, MAX(w.started_at) AS at $DONE"

private const val SETS_OF_LATEST = """
    JOIN workout_set s ON s.workout_exercise_id = latest.id
    WHERE s.is_completed = 1 AND s.deleted_at IS NULL
    ORDER BY latest.exercise_id, s.position
"""

/** Read-only questions about finished workouts (SPEC §10: well under 16 ms). */
@Dao
interface HistoryDao {

    @Query("SELECT we.exercise_id, we.workout_id, w.started_at AS at $DONE AND w.started_at >= :since")
    suspend fun usages(since: Long): List<UsageRow>

    @Query(
        "SELECT latest.exercise_id, latest.workout_id, latest.at, $SET_COLUMNS " +
            "FROM ($LATEST AND w.started_at < :before GROUP BY we.exercise_id) latest $SETS_OF_LATEST",
    )
    suspend fun latestSets(before: Long): List<SessionSetRow>

    @Query(
        "SELECT latest.exercise_id, latest.workout_id, latest.at, $SET_COLUMNS " +
            "FROM ($LATEST AND w.started_at < :before AND we.exercise_id IN (:ids) GROUP BY we.exercise_id) latest " +
            SETS_OF_LATEST,
    )
    suspend fun latestSets(before: Long, ids: List<String>): List<SessionSetRow>

    @Query(
        "SELECT session.exercise_id, session.workout_id, session.at, $SET_COLUMNS " +
            "FROM (SELECT we.id, we.exercise_id, we.workout_id, w.started_at AS at $DONE " +
            "AND we.exercise_id = :exerciseId ORDER BY w.started_at DESC LIMIT :limit) session " +
            "JOIN workout_set s ON s.workout_exercise_id = session.id " +
            "WHERE s.is_completed = 1 AND s.deleted_at IS NULL ORDER BY session.at DESC, session.id, s.position",
    )
    fun observeSessionSets(exerciseId: String, limit: Int): Flow<List<SessionSetRow>>

    /** When each routine was last done. */
    @Query(
        "SELECT routine_id, MAX(started_at) AS at FROM workout " +
            "WHERE routine_id IS NOT NULL AND is_in_progress = 0 AND deleted_at IS NULL GROUP BY routine_id",
    )
    fun observeRoutineUse(): Flow<List<RoutineUseRow>>
}

data class UsageRow(
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    val at: Long,
)

/** A completed set with the session it belongs to. */
data class SessionSetRow(
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

data class RoutineUseRow(@ColumnInfo(name = "routine_id") val routineId: String, val at: Long)
