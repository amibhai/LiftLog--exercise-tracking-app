package io.github.amibhai.liftlog.core.testing

import io.github.amibhai.liftlog.core.domain.backup.BackupSettings
import io.github.amibhai.liftlog.core.domain.backup.BackupSummary
import io.github.amibhai.liftlog.core.domain.images.ImageDownload
import io.github.amibhai.liftlog.core.domain.images.OfflineImages
import io.github.amibhai.liftlog.core.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Records what it was asked to do; everything works unless [works] says otherwise. */
class FakeBackupRepository : BackupRepository {
    override val settings = MutableStateFlow(BackupSettings())
    val calls = mutableListOf<String>()
    var works = true
    var summary: BackupSummary? = null
    val texts = mutableMapOf<String, String>()

    override suspend fun updateSettings(transform: (BackupSettings) -> BackupSettings) = settings.update(transform)

    override suspend fun chooseFolder(uri: String): Boolean {
        if (works) settings.update { it.copy(folderUri = uri) }
        return call("folder $uri")
    }

    override suspend fun backUpNow(): Boolean = call("backup")

    override suspend fun exportJson(uri: String): Boolean = call("json $uri")

    override suspend fun exportCsv(uri: String): Boolean = call("csv $uri")

    override suspend fun summaryOf(uri: String): BackupSummary? = summary

    override suspend fun restore(uri: String): Boolean = call("restore $uri")

    override suspend fun undoRestore(): Boolean = call("undo")

    override suspend fun readText(uri: String): String? = texts[uri]

    private fun call(name: String): Boolean {
        calls += name
        return works
    }
}

class FakeOfflineImages : OfflineImages {
    val state = MutableStateFlow(ImageDownload(done = 0, total = 10, isRunning = false))

    override fun observe(): Flow<ImageDownload> = state

    override fun download() = state.update { it.copy(isRunning = true) }

    override fun cancel() = state.update { it.copy(isRunning = false) }
}
