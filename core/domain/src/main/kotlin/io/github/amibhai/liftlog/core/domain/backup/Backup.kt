package io.github.amibhai.liftlog.core.domain.backup

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PREFIX = "liftlog-backup-"
private const val EXTENSION = ".json"
private val NameTime = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss", Locale.ROOT)

enum class BackupFrequency { OFF, DAILY, WEEKLY }

/** Automatic backups (SPEC §9). They belong to this phone and are not part of a backup themselves. */
data class BackupSettings(
    /** A folder chosen through the system; null when there is none yet. */
    val folderUri: String? = null,
    val folderName: String? = null,
    val frequency: BackupFrequency = BackupFrequency.DAILY,
    /** How many backups to keep. */
    val keep: Int = DEFAULT_KEEP,
    val lastBackupAt: Long? = null,
    /** The last automatic backup did not work, for example because the folder is gone. */
    val lastFailed: Boolean = false,
) {
    val isAutomatic: Boolean get() = folderUri != null && frequency != BackupFrequency.OFF

    companion object {
        const val DEFAULT_KEEP = 10
        const val MOST_KEEP = 50
    }
}

/** What a backup contains, to decide whether to restore it. */
data class BackupSummary(
    val createdAt: Long,
    val workouts: Int,
    val routines: Int,
    val programs: Int,
    val customExercises: Int,
    val measurements: Int,
)

/** "liftlog-backup-2026-11-24-213005.json": names in order of time. */
fun backupFileName(now: Long, zone: ZoneId): String =
    PREFIX + NameTime.format(Instant.ofEpochMilli(now).atZone(zone)) + EXTENSION

/** The backups among [names] that go when only the newest [keep] stay. Other files are left alone. */
fun backupsToDelete(names: List<String>, keep: Int): List<String> =
    names.filter { it.startsWith(PREFIX) && it.endsWith(EXTENSION) }.sortedDescending().drop(keep)
