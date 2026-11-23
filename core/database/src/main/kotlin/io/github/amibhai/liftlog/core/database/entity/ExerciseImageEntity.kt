package io.github.amibhai.liftlog.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A remote exercise image; never bundled in the APK (SPEC §3). Each image carries its own license. */
@Entity(
    tableName = "exercise_image",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exercise_id", "position"])],
)
data class ExerciseImageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val position: Int,
    val url: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
    val license: String?,
    val author: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)
