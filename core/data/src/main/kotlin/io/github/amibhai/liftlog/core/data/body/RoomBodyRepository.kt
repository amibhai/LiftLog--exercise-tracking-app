package io.github.amibhai.liftlog.core.data.body

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amibhai.liftlog.core.data.di.IoDispatcher
import io.github.amibhai.liftlog.core.database.dao.BodyDao
import io.github.amibhai.liftlog.core.database.entity.BodyMetricEntity
import io.github.amibhai.liftlog.core.database.entity.ProgressPhotoEntity
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.model.ProgressPhotoId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.ProgressPhoto
import io.github.amibhai.liftlog.core.domain.repository.BodyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val PHOTO_FOLDER = "progress_photos"
private const val PHOTO_EXTENSION = "jpg"

/** Deleted pictures can be brought back for this long; then their files go. */
private val KeepDeletedPhotos = TimeUnit.DAYS.toMillis(7)

internal class RoomBodyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: BodyDao,
    private val ids: IdGenerator,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BodyRepository {

    private val folder: File get() = File(context.filesDir, PHOTO_FOLDER)

    override fun observeMetrics(type: BodyMetricType): Flow<List<BodyMetric>> =
        dao.observeMetrics(type.name).map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeLatestMetrics(): Flow<List<BodyMetric>> =
        dao.observeLatestMetrics().map { rows -> rows.mapNotNull { it.toDomain() }.sortedBy { it.type } }

    override suspend fun saveMetric(metric: BodyMetric) {
        val now = clock.millis()
        val created = dao.metric(metric.id.value)?.createdAt ?: now
        dao.upsertMetric(
            BodyMetricEntity(
                id = metric.id.value,
                metricType = metric.type.name,
                value = metric.value,
                measuredAt = metric.measuredAt,
                notes = metric.notes,
                createdAt = created,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    override suspend fun deleteMetric(id: BodyMetricId) = setMetricDeleted(id, clock.millis())

    override suspend fun restoreMetric(id: BodyMetricId) = setMetricDeleted(id, deletedAt = null)

    /** A restored database can know of pictures that did not come along; those are left out. */
    override fun observePhotos(): Flow<List<ProgressPhoto>> = dao.observePhotos().map { rows ->
        rows.map { ProgressPhoto(ProgressPhotoId(it.id), it.takenAt, File(folder, it.fileName).path) }
            .filter { File(it.path).exists() }
    }.flowOn(io)

    override suspend fun addPhoto(sourceUri: String, takenAt: Long): ProgressPhoto? = withContext(io) {
        purgeDeletedPhotos()
        val id = ids.newId()
        val file = File(folder.apply { mkdirs() }, "$id.$PHOTO_EXTENSION")
        if (!copy(sourceUri.toUri(), file)) return@withContext null
        val now = clock.millis()
        dao.upsertPhoto(
            ProgressPhotoEntity(
                id = id,
                takenAt = takenAt,
                fileName = file.name,
                pose = null,
                notes = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        ProgressPhoto(ProgressPhotoId(id), takenAt, file.path)
    }

    override suspend fun deletePhoto(id: ProgressPhotoId) = setPhotoDeleted(id, clock.millis())

    override suspend fun restorePhoto(id: ProgressPhotoId) = setPhotoDeleted(id, deletedAt = null)

    private suspend fun setMetricDeleted(id: BodyMetricId, deletedAt: Long?) =
        dao.setMetricDeleted(id.value, deletedAt, clock.millis())

    private suspend fun setPhotoDeleted(id: ProgressPhotoId, deletedAt: Long?) =
        dao.setPhotoDeleted(id.value, deletedAt, clock.millis())

    private suspend fun purgeDeletedPhotos() {
        val old = dao.photosDeletedBefore(clock.millis() - KeepDeletedPhotos)
        old.forEach { File(folder, it.fileName).delete() }
        if (old.isNotEmpty()) dao.purgePhotos(old.map { it.id })
    }

    /** False when the picture cannot be read. */
    private fun copy(source: Uri, target: File): Boolean = try {
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } != null
    } catch (_: IOException) {
        target.delete()
        false
    } catch (_: SecurityException) {
        target.delete()
        false
    }

    private fun BodyMetricEntity.toDomain(): BodyMetric? = BodyMetricType.entries.firstOrNull { it.name == metricType }
        ?.let { BodyMetric(BodyMetricId(id), it, value, measuredAt, notes) }
}
