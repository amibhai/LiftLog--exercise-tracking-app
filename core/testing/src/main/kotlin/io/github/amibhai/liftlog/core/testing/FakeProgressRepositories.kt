package io.github.amibhai.liftlog.core.testing

import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ProgressPhotoId
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.progress.ProgressPhoto
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.domain.repository.BodyRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeProgressRepository : ProgressRepository {
    val sessions = MutableStateFlow(emptyMap<ExerciseId, List<ExerciseSession>>())
    val stats = MutableStateFlow(emptyList<WorkoutStats>())

    /** The same for every time span. */
    val work = MutableStateFlow(emptyList<MuscleWork>())
    val workOf = MutableStateFlow(emptyMap<WorkoutId, List<MuscleWork>>())
    val recentRecords = MutableStateFlow(emptyList<RecordEntry>())
    val recordsOf = MutableStateFlow(emptyMap<WorkoutId, List<RecordEntry>>())
    var rebuilds = 0
        private set

    override fun observeSessions(id: ExerciseId): Flow<List<ExerciseSession>> = sessions.map { it[id].orEmpty() }

    override suspend fun sessionsBefore(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, List<ExerciseSession>> =
        sessions.value
            .filterKeys { it in ids }
            .mapValues { (_, list) -> list.filter { it.at < before } }

    override fun observeWorkoutStats(): Flow<List<WorkoutStats>> = stats

    override fun observeMuscleWork(from: Long, to: Long): Flow<List<MuscleWork>> = work

    override fun observeMuscleWork(workout: WorkoutId): Flow<List<MuscleWork>> = workOf.map { it[workout].orEmpty() }

    override fun observeRecentRecords(limit: Int): Flow<List<RecordEntry>> = recentRecords.map { it.take(limit) }

    override fun observeRecordsOf(workout: WorkoutId): Flow<List<RecordEntry>> = recordsOf.map { it[workout].orEmpty() }

    override suspend fun rebuildRecords() {
        rebuilds++
    }
}

class FakeBodyRepository : BodyRepository {
    private val metrics = MutableStateFlow(emptyMap<BodyMetricId, BodyMetric>())
    private val deleted = mutableMapOf<BodyMetricId, BodyMetric>()
    private val photos = MutableStateFlow(emptyList<ProgressPhoto>())
    private val deletedPhotos = mutableMapOf<ProgressPhotoId, ProgressPhoto>()
    var photoToAdd: ProgressPhoto? = null

    override fun observeMetrics(type: BodyMetricType): Flow<List<BodyMetric>> =
        metrics.map { all -> all.values.filter { it.type == type }.sortedBy { it.measuredAt } }

    override fun observeLatestMetrics(): Flow<List<BodyMetric>> = metrics.map { all ->
        all.values.groupBy { it.type }.values.map { list -> list.maxBy { it.measuredAt } }.sortedBy { it.type }
    }

    override suspend fun saveMetric(metric: BodyMetric) = metrics.update { it + (metric.id to metric) }

    override suspend fun deleteMetric(id: BodyMetricId) {
        metrics.value[id]?.let { deleted[id] = it }
        metrics.update { it - id }
    }

    override suspend fun restoreMetric(id: BodyMetricId) {
        deleted.remove(id)?.let { metric -> metrics.update { it + (id to metric) } }
    }

    override fun observePhotos(): Flow<List<ProgressPhoto>> = photos

    override suspend fun addPhoto(sourceUri: String, takenAt: Long): ProgressPhoto? =
        photoToAdd?.copy(takenAt = takenAt)?.also { photo -> photos.update { listOf(photo) + it } }

    override suspend fun deletePhoto(id: ProgressPhotoId) {
        photos.value.firstOrNull { it.id == id }?.let { deletedPhotos[id] = it }
        photos.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun restorePhoto(id: ProgressPhotoId) {
        deletedPhotos.remove(id)?.let { photo -> photos.update { listOf(photo) + it } }
    }
}
