package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import kotlinx.coroutines.flow.Flow

/** Questions about finished workouts. */
interface HistoryRepository {
    /** Which exercises were done in which workouts since [since]; the input of frecency. */
    suspend fun usages(since: Long): List<ExerciseUsage>

    /** The latest session of each exercise before [before]: the "Previous" column and "Last: 80 kg × 8". */
    suspend fun latestSessions(before: Long): Map<ExerciseId, ExerciseSession>

    /** The same for a few exercises only. */
    suspend fun latestSessions(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, ExerciseSession>

    /** The [limit] most recent sessions of one exercise, newest first. */
    fun observeSessions(id: ExerciseId, limit: Int): Flow<List<ExerciseSession>>
}
