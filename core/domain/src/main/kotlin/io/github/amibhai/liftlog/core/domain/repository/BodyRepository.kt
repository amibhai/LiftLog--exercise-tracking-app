package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.model.ProgressPhotoId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.ProgressPhoto
import kotlinx.coroutines.flow.Flow

/** Body weight, measurements and progress photos (SPEC §8). Photos never leave the app's private storage. */
interface BodyRepository {
    /** One kind of measurement, oldest first. */
    fun observeMetrics(type: BodyMetricType): Flow<List<BodyMetric>>

    /** The newest measurement of every kind that has one. */
    fun observeLatestMetrics(): Flow<List<BodyMetric>>

    suspend fun saveMetric(metric: BodyMetric)

    /** Deletes softly, so that [restoreMetric] can undo it. */
    suspend fun deleteMetric(id: BodyMetricId)

    suspend fun restoreMetric(id: BodyMetricId)

    /** Newest first. */
    fun observePhotos(): Flow<List<ProgressPhoto>>

    /** Copies the picture behind [sourceUri] into private storage; null when it cannot be read. */
    suspend fun addPhoto(sourceUri: String, takenAt: Long): ProgressPhoto?

    suspend fun deletePhoto(id: ProgressPhotoId)

    suspend fun restorePhoto(id: ProgressPhotoId)
}
