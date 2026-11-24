package io.github.amibhai.liftlog.feature.settings.data

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.backup.BackupFrequency
import io.github.amibhai.liftlog.core.domain.backup.BackupSettings
import io.github.amibhai.liftlog.core.domain.backup.BackupSummary
import io.github.amibhai.liftlog.core.domain.backup.backupFileName
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.images.ImageDownload
import io.github.amibhai.liftlog.core.domain.images.OfflineImages
import io.github.amibhai.liftlog.core.domain.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class DataUiState(
    /** Null until loaded. */
    val backup: BackupSettings? = null,
    val images: ImageDownload? = null,
    /** Something is being written or read right now. */
    val isBusy: Boolean = false,
    val restore: RestoreOffer? = null,
    val message: DataMessage? = null,
)

/** A backup that was picked, to be confirmed before it replaces everything. */
@Immutable
data class RestoreOffer(val uri: String, val summary: BackupSummary)

/** How something went. */
enum class DataMessage {
    BACKED_UP,
    BACKUP_FAILED,
    FOLDER_FAILED,
    EXPORTED,
    EXPORT_FAILED,
    NOT_A_BACKUP,
    RESTORED,
    RESTORE_FAILED,
    RESTORE_UNDONE,
}

/** Backups, exports, imports and offline pictures (SPEC §9, §12). */
@HiltViewModel
class DataViewModel @Inject constructor(
    private val backups: BackupRepository,
    private val images: OfflineImages,
    private val work: SequentialWork,
    private val clock: Clock,
) : ViewModel() {

    private data class Local(
        val isBusy: Boolean = false,
        val restore: RestoreOffer? = null,
        val message: DataMessage? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<DataUiState> =
        combine(backups.settings, images.observe(), local) { backup, images, local ->
            DataUiState(backup, images, local.isBusy, local.restore, local.message)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DataUiState())

    /** A name for a new document. */
    val jsonName: String get() = backupFileName(clock.millis(), ZoneId.systemDefault())

    fun setSchedule(frequency: BackupFrequency, keep: Int) {
        viewModelScope.launch { backups.updateSettings { it.copy(frequency = frequency, keep = keep) } }
    }

    /** The first backup follows at once, so that a folder that does not work is noticed. */
    fun chooseFolder(uri: String) = perform {
        when {
            !backups.chooseFolder(uri) -> DataMessage.FOLDER_FAILED
            backups.backUpNow() -> DataMessage.BACKED_UP
            else -> DataMessage.BACKUP_FAILED
        }
    }

    fun backUpNow() = perform { if (backups.backUpNow()) DataMessage.BACKED_UP else DataMessage.BACKUP_FAILED }

    fun exportJson(uri: String) = perform { exported(backups.exportJson(uri)) }

    fun exportCsv(uri: String) = perform { exported(backups.exportCsv(uri)) }

    /** Shows what the backup contains before it replaces anything. */
    fun offerRestore(uri: String) = perform {
        val summary = backups.summaryOf(uri)
        if (summary != null) local.update { it.copy(restore = RestoreOffer(uri, summary)) }
        if (summary == null) DataMessage.NOT_A_BACKUP else null
    }

    fun dismissRestore() = local.update { it.copy(restore = null) }

    fun restore() {
        val offer = local.value.restore ?: return
        dismissRestore()
        perform { if (backups.restore(offer.uri)) DataMessage.RESTORED else DataMessage.RESTORE_FAILED }
    }

    fun undoRestore() = perform {
        if (backups.undoRestore()) DataMessage.RESTORE_UNDONE else DataMessage.RESTORE_FAILED
    }

    fun dismissMessage() = local.update { it.copy(message = null) }

    /** Downloads the pictures that are missing, or stops doing so. */
    fun toggleImageDownload() {
        if (uiState.value.images?.isRunning == true) images.cancel() else images.download()
    }

    private fun exported(isDone: Boolean) = if (isDone) DataMessage.EXPORTED else DataMessage.EXPORT_FAILED

    /** In the queue of the app: it goes on when the screen is left, and waits for edits made before. */
    private fun perform(task: suspend () -> DataMessage?) {
        local.update { it.copy(isBusy = true, message = null) }
        work.submit {
            val message = task()
            local.update { it.copy(isBusy = false, message = message) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
