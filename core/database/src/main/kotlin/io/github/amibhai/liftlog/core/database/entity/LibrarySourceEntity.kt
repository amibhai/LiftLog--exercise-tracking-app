package io.github.amibhai.liftlog.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A data source of the built-in library, for the About & attributions screen. */
@Entity(tableName = "library_source")
data class LibrarySourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val license: String,
    @ColumnInfo(name = "license_url") val licenseUrl: String?,
    val revision: String?,
    val notice: String?,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)
