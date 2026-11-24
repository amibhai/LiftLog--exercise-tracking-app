package io.github.amibhai.liftlog.core.data.backup

import android.content.Context
import android.database.SQLException
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amibhai.liftlog.core.data.di.IoDispatcher
import io.github.amibhai.liftlog.core.domain.backup.BackupSettings
import io.github.amibhai.liftlog.core.domain.backup.BackupSummary
import io.github.amibhai.liftlog.core.domain.backup.backupFileName
import io.github.amibhai.liftlog.core.domain.backup.setsCsv
import io.github.amibhai.liftlog.core.domain.repository.BackupRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** What the data was before the last restore; private to the app. */
private const val UNDO_FILE = "before-restore.json"

/** Nobody has a training log larger than this; something else was picked by mistake. */
private const val MAX_IMPORT_BYTES = 32 * 1024 * 1024

@Singleton
internal class AndroidBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: BackupStore,
    private val folder: BackupFolder,
    private val dataStore: DataStore<BackupSettingsJson>,
    private val workouts: WorkoutRepository,
    private val userSettings: SettingsRepository,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BackupRepository {

    private val undoFile: File get() = File(context.filesDir, UNDO_FILE)

    override val settings: Flow<BackupSettings> = dataStore.data.map { it.toDomain() }.distinctUntilChanged()

    override suspend fun updateSettings(transform: (BackupSettings) -> BackupSettings) {
        dataStore.updateData { transform(it.toDomain()).toJson() }
    }

    override suspend fun chooseFolder(uri: String): Boolean = succeeds {
        val tree = uri.toUri()
        folder.keepAccess(tree, settings.first().folderUri?.toUri())
        val name = folder.nameOf(tree)
        updateSettings { it.copy(folderUri = uri, folderName = name, lastFailed = false) }
    }

    override suspend fun backUpNow(): Boolean {
        val current = settings.first()
        val tree = current.folderUri?.toUri() ?: return false
        val isDone = succeeds {
            folder.write(tree, backupFileName(clock.millis(), ZoneId.systemDefault())) { store.write(it) }
            folder.prune(tree, current.keep)
        }
        updateSettings {
            it.copy(lastBackupAt = if (isDone) clock.millis() else it.lastBackupAt, lastFailed = !isDone)
        }
        return isDone
    }

    override suspend fun exportJson(uri: String): Boolean = succeeds { open(uri).use { store.write(it) } }

    override suspend fun exportCsv(uri: String): Boolean = succeeds {
        val unit = userSettings.settings.first().weightUnit
        val csv = setsCsv(workouts.observeFinished(Int.MAX_VALUE).first(), unit, ZoneId.systemDefault())
        open(uri).use { it.write(csv.encodeToByteArray()) }
    }

    override suspend fun summaryOf(uri: String): BackupSummary? = read(uri)?.summary()

    override suspend fun restore(uri: String): Boolean {
        val file = read(uri) ?: return false
        return succeeds {
            undoFile.outputStream().use { store.write(it) }
            store.restore(file)
        }
    }

    override suspend fun undoRestore(): Boolean = succeeds {
        val file = undoFile.inputStream().use { store.read(it) } ?: throw IOException("Nothing to undo")
        store.restore(file)
        undoFile.delete()
    }

    override suspend fun readText(uri: String): String? {
        var text: String? = null
        succeeds {
            text = context.contentResolver.openInputStream(uri.toUri())?.use { it.readText(MAX_IMPORT_BYTES) }
        }
        return text
    }

    private suspend fun read(uri: String): BackupFile? {
        var file: BackupFile? = null
        succeeds { file = context.contentResolver.openInputStream(uri.toUri())?.use { store.read(it) } }
        return file
    }

    /** "wt" truncates: a shorter export must not leave the rest of a longer one behind. */
    private fun open(uri: String): OutputStream =
        context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw IOException("Cannot write $uri")

    /** Documents belong to other apps, which can refuse or break in their own ways. */
    private suspend fun succeeds(task: suspend () -> Unit): Boolean = withContext(io) {
        try {
            task()
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: SQLException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IllegalStateException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        }
    }
}

private fun InputStream.readText(most: Int): String {
    val bytes = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    generateSequence { read(buffer).takeIf { it >= 0 } }.forEach { count ->
        bytes.write(buffer, 0, count)
        if (bytes.size() > most) throw IOException("Too large")
    }
    return bytes.toByteArray().decodeToString()
}
