package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import io.github.amibhai.liftlog.core.database.entity.BodyMetricEntity
import io.github.amibhai.liftlog.core.database.entity.ProgressPhotoEntity
import kotlinx.coroutines.flow.Flow

/** Body weight, measurements and progress photos (SPEC §8). */
@Dao
interface BodyDao {

    @Query("SELECT * FROM body_metric WHERE metric_type = :type AND deleted_at IS NULL ORDER BY measured_at, id")
    fun observeMetrics(type: String): Flow<List<BodyMetricEntity>>

    /** The newest measurement of every kind. */
    @Query(
        """
        SELECT * FROM body_metric m WHERE m.deleted_at IS NULL AND m.id = (
            SELECT l.id FROM body_metric l WHERE l.metric_type = m.metric_type AND l.deleted_at IS NULL
            ORDER BY l.measured_at DESC, l.id DESC LIMIT 1
        )
        """,
    )
    fun observeLatestMetrics(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metric WHERE id = :id")
    suspend fun metric(id: String): BodyMetricEntity?

    @Upsert
    suspend fun upsertMetric(metric: BodyMetricEntity)

    @Query("UPDATE body_metric SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setMetricDeleted(id: String, deletedAt: Long?, now: Long)

    @Query("SELECT * FROM progress_photo WHERE deleted_at IS NULL ORDER BY taken_at DESC, id")
    fun observePhotos(): Flow<List<ProgressPhotoEntity>>

    @Upsert
    suspend fun upsertPhoto(photo: ProgressPhotoEntity)

    @Query("UPDATE progress_photo SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setPhotoDeleted(id: String, deletedAt: Long?, now: Long)

    /** Pictures deleted before [before], whose files can go. */
    @Query("SELECT * FROM progress_photo WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun photosDeletedBefore(before: Long): List<ProgressPhotoEntity>

    @Query("DELETE FROM progress_photo WHERE id IN (:ids)")
    suspend fun purgePhotos(ids: List<String>)
}
