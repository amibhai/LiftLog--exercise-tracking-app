package io.github.amibhai.liftlog.core.data.progress

import io.github.amibhai.liftlog.core.database.dao.ExerciseSetsRow
import io.github.amibhai.liftlog.core.database.dao.ProgressDao
import io.github.amibhai.liftlog.core.database.dao.RecordRow
import io.github.amibhai.liftlog.core.domain.model.ExerciseHistory
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.domain.progression.PlannedSession
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

internal class RoomProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val keeper: RecordKeeper,
) : ProgressRepository {

    override fun observeSessions(id: ExerciseId): Flow<List<ExerciseSession>> =
        dao.observeSets(id.value).map { it.toSessions() }.distinctUntilChanged()

    override fun observeLoadedExercises(since: Long): Flow<List<ExerciseHistory>> =
        dao.observeLoadedSets(since).map { it.toHistories() }.distinctUntilChanged()

    override suspend fun sessionsBefore(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, List<ExerciseSession>> =
        if (ids.isEmpty()) {
            emptyMap()
        } else {
            dao.setsBefore(ids.map { it.value }, before)
                .groupBy { ExerciseId(it.exerciseId) }
                .mapValues { (_, rows) -> rows.toSessions() }
        }

    override suspend fun plannedSessions(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, List<PlannedSession>> =
        if (ids.isEmpty()) {
            emptyMap()
        } else {
            dao.plannedSetsBefore(ids.map { it.value }, before)
                .groupBy { ExerciseId(it.set.exerciseId) }
                .mapValues { (_, rows) -> rows.toPlannedSessions() }
        }

    override fun observeWorkoutStats(): Flow<List<WorkoutStats>> = dao.observeWorkoutStats().map { rows ->
        rows.map { WorkoutStats(WorkoutId(it.id), it.startedAt, it.endedAt ?: it.startedAt, it.sets, it.volume) }
    }.distinctUntilChanged()

    override fun observeMuscleWork(from: Long, to: Long): Flow<List<MuscleWork>> =
        dao.observeExerciseSets(from, to).toWork()

    override fun observeMuscleWork(workout: WorkoutId): Flow<List<MuscleWork>> =
        dao.observeExerciseSets(workout.value).toWork()

    override fun observeRecentRecords(limit: Int): Flow<List<RecordEntry>> =
        dao.observeRecentRecords(limit).map { rows -> rows.mapNotNull(RecordRow::toDomain) }.distinctUntilChanged()

    override fun observeRecordsOf(workout: WorkoutId): Flow<List<RecordEntry>> =
        dao.observeRecordsOf(workout.value).map { rows -> rows.mapNotNull(RecordRow::toDomain) }.distinctUntilChanged()

    override suspend fun rebuildRecords() = keeper.rebuildAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<List<ExerciseSetsRow>>.toWork(): Flow<List<MuscleWork>> = mapLatest { rows ->
        val muscles = dao.muscles(rows.map { it.exerciseId }).groupBy { it.exerciseId }
        rows.map { row ->
            val (primary, secondary) = muscles[row.exerciseId].orEmpty().partition { it.isPrimary }
            MuscleWork(
                exerciseId = ExerciseId(row.exerciseId),
                primary = primary.map { it.muscle },
                secondary = secondary.map { it.muscle },
                sets = row.sets,
            )
        }
    }.distinctUntilChanged()
}
