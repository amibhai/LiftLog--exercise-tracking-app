package io.github.amibhai.liftlog.core.data.workout

import io.github.amibhai.liftlog.core.database.dao.HistoryDao
import io.github.amibhai.liftlog.core.database.dao.SessionSetRow
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.units.Weight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class RoomHistoryRepository @Inject constructor(private val dao: HistoryDao) : HistoryRepository {

    override suspend fun usages(since: Long): List<ExerciseUsage> =
        dao.usages(since).map { ExerciseUsage(ExerciseId(it.exerciseId), WorkoutId(it.workoutId), it.at) }

    override suspend fun latestSessions(before: Long): Map<ExerciseId, ExerciseSession> =
        dao.latestSets(before).byExercise()

    override suspend fun latestSessions(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, ExerciseSession> =
        if (ids.isEmpty()) emptyMap() else dao.latestSets(before, ids.map { it.value }).byExercise()

    override fun observeSessions(id: ExerciseId, limit: Int): Flow<List<ExerciseSession>> =
        dao.observeSessionSets(id.value, limit)
            .map { rows -> rows.groupBy { it.workoutId }.values.map { it.toSession() } }
            .distinctUntilChanged()

    private fun List<SessionSetRow>.byExercise(): Map<ExerciseId, ExerciseSession> =
        groupBy { ExerciseId(it.exerciseId) }.mapValues { (_, rows) -> rows.toSession() }

    private fun List<SessionSetRow>.toSession() = ExerciseSession(
        workoutId = WorkoutId(first().workoutId),
        at = first().at,
        sets = map {
            LoggedSet(
                type = it.type,
                values = SetValues(it.weightKg?.let(::Weight), it.reps, it.durationSeconds, it.distanceMeters),
                rpe = it.rpe,
                rir = it.rir,
            )
        },
    )
}
