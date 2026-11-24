package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.backup.BackupSettings
import io.github.amibhai.liftlog.core.domain.backup.BackupSummary
import kotlinx.coroutines.flow.Flow

/**
 * Backups, exports and imports (SPEC §9). Documents are the URIs the system's file picker hands out.
 * Nothing here throws: what cannot be read or written is reported with false or null.
 */
interface BackupRepository {
    val settings: Flow<BackupSettings>

    suspend fun updateSettings(transform: (BackupSettings) -> BackupSettings)

    /** Keeps the access to the folder at [uri] for automatic backups. */
    suspend fun chooseFolder(uri: String): Boolean

    /** Writes a backup into the chosen folder and removes the oldest ones. */
    suspend fun backUpNow(): Boolean

    /** Everything but the progress photos, as JSON. */
    suspend fun exportJson(uri: String): Boolean

    /** Every completed set as a row. */
    suspend fun exportCsv(uri: String): Boolean

    /** Null when the document is no LiftLog backup, or one of a newer version. */
    suspend fun summaryOf(uri: String): BackupSummary?

    /** Replaces everything but the progress photos with the backup. */
    suspend fun restore(uri: String): Boolean

    /** Takes the last [restore] back. */
    suspend fun undoRestore(): Boolean

    /** The text of a document, for imports. */
    suspend fun readText(uri: String): String?
}
