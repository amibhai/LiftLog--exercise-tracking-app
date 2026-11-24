package io.github.amibhai.liftlog.feature.settings.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.backup.BackupFrequency
import io.github.amibhai.liftlog.core.domain.backup.BackupSettings
import io.github.amibhai.liftlog.core.domain.backup.BackupSummary
import io.github.amibhai.liftlog.core.domain.images.ImageDownload
import io.github.amibhai.liftlog.core.ui.component.ConfirmDialog
import io.github.amibhai.liftlog.core.ui.component.NumberStepper
import io.github.amibhai.liftlog.core.ui.format.dateTimeText
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.ChoiceRow
import io.github.amibhai.liftlog.feature.settings.R
import io.github.amibhai.liftlog.feature.settings.SectionHeader
import io.github.amibhai.liftlog.feature.settings.SettingsPage
import io.github.amibhai.liftlog.core.ui.R as UiR

private const val JSON = "application/json"
private const val CSV = "text/csv"
private const val CSV_NAME = "liftlog-sets.csv"

/** Other apps name the type of a CSV or JSON file in many ways, so every file can be picked. */
private val AnyFile = arrayOf("*/*")

@Composable
internal fun DataRoute(onBack: () -> Unit, onImport: (String) -> Unit, viewModel: DataViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val import by rememberUpdatedState(onImport)
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.chooseFolder(it.toString()) }
    }
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        uri?.let { viewModel.exportJson(it.toString()) }
    }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV)) { uri ->
        uri?.let { viewModel.exportCsv(it.toString()) }
    }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.offerRestore(it.toString()) }
    }
    val log = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { import(it.toString()) }
    }
    val actions = DataActions(
        onChooseFolder = { folder.launch(null) },
        onScheduleChange = viewModel::setSchedule,
        onBackUpNow = viewModel::backUpNow,
        onExportJson = { json.launch(viewModel.jsonName) },
        onExportCsv = { csv.launch(CSV_NAME) },
        onPickBackup = { backup.launch(AnyFile) },
        onRestore = viewModel::restore,
        onDismissRestore = viewModel::dismissRestore,
        onUndoRestore = viewModel::undoRestore,
        onPickImport = { log.launch(AnyFile) },
        onToggleImages = viewModel::toggleImageDownload,
        onDismissMessage = viewModel::dismissMessage,
    )
    DataScreen(state = state, actions = actions, onBack = onBack)
}

@Composable
internal fun DataScreen(state: DataUiState, actions: DataActions, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val snackbars = remember { SnackbarHostState() }
    MessageSnackbar(state.message, snackbars, actions)
    SettingsPage(
        title = stringResource(R.string.data_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.backup?.let { BackupSection(it, isBusy = state.isBusy, actions) }
            SectionHeader(stringResource(R.string.data_section_export))
            ActionRow(R.string.data_export_json, R.string.data_export_json_body, state, actions.onExportJson)
            ActionRow(R.string.data_export_csv, R.string.data_export_csv_body, state, actions.onExportCsv)
            SectionHeader(stringResource(R.string.data_section_import))
            ActionRow(R.string.data_restore, R.string.data_restore_body, state, actions.onPickBackup)
            ActionRow(R.string.data_import, R.string.data_import_body, state, actions.onPickImport)
            SectionHeader(stringResource(R.string.data_section_images))
            state.images?.let { ImagesRow(it, actions) }
        }
    }
    state.restore?.let { offer ->
        ConfirmDialog(
            title = stringResource(R.string.data_restore_title),
            text = restoreText(offer.summary),
            confirmLabel = stringResource(R.string.data_restore_confirm),
            onConfirm = actions.onRestore,
            onDismiss = actions.onDismissRestore,
            isDestructive = true,
        )
    }
}

@Composable
private fun BackupSection(
    backup: BackupSettings,
    isBusy: Boolean,
    actions: DataActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(stringResource(R.string.data_section_backup))
        ListItem(
            onClick = actions.onChooseFolder,
            supportingContent = {
                Text(
                    when {
                        backup.folderUri == null -> stringResource(R.string.data_folder_none)
                        else -> backup.folderName ?: stringResource(R.string.data_folder_unnamed)
                    },
                )
            },
        ) { Text(stringResource(R.string.data_folder)) }
        if (backup.folderUri != null) {
            ChoiceRow(
                title = stringResource(R.string.data_frequency),
                options = BackupFrequency.entries,
                selected = backup.frequency,
                label = { stringResource(it.labelRes()) },
                onSelect = { actions.onScheduleChange(it, backup.keep) },
            )
            ListItem(
                trailingContent = {
                    NumberStepper(
                        value = backup.keep,
                        range = 1..BackupSettings.MOST_KEEP,
                        onChange = { actions.onScheduleChange(backup.frequency, it) },
                        fewer = stringResource(R.string.data_keep_fewer),
                        more = stringResource(R.string.data_keep_more),
                    )
                },
            ) { Text(stringResource(R.string.data_keep)) }
            ListItem(
                onClick = actions.onBackUpNow,
                enabled = !isBusy,
                supportingContent = { Text(lastBackupText(backup)) },
            ) { Text(stringResource(R.string.data_back_up_now)) }
        }
    }
}

@Composable
private fun lastBackupText(backup: BackupSettings): String {
    val last = backup.lastBackupAt?.let { stringResource(R.string.data_last_backup, dateTimeText(it)) }
        ?: stringResource(R.string.data_no_backup)
    return if (backup.lastFailed) stringResource(R.string.data_last_failed) + " " + last else last
}

@Composable
private fun ActionRow(
    @StringRes title: Int,
    @StringRes body: Int,
    state: DataUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        enabled = !state.isBusy,
        supportingContent = { Text(stringResource(body)) },
    ) { Text(stringResource(title)) }
}

@Composable
private fun ImagesRow(images: ImageDownload, actions: DataActions, modifier: Modifier = Modifier) {
    ListItem(
        onClick = actions.onToggleImages,
        modifier = modifier,
        supportingContent = {
            Column {
                Text(
                    text = when {
                        images.isRunning -> stringResource(R.string.data_images_running, images.done, images.total)
                        images.done >= images.total -> stringResource(R.string.data_images_complete)
                        else -> stringResource(R.string.data_images_body, images.done, images.total)
                    },
                )
                if (images.isRunning && images.total > 0) {
                    LinearProgressIndicator(
                        progress = { images.done.toFloat() / images.total },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LiftLogDimens.spacingS),
                    )
                }
            }
        },
    ) {
        Text(stringResource(if (images.isRunning) R.string.data_images_cancel else R.string.data_images))
    }
}

@Composable
private fun restoreText(summary: BackupSummary): String {
    val contents = listOf(
        pluralStringResource(R.plurals.data_workouts, summary.workouts, summary.workouts),
        pluralStringResource(R.plurals.data_routines, summary.routines, summary.routines),
        pluralStringResource(R.plurals.data_measurements, summary.measurements, summary.measurements),
    ).joinToString()
    return stringResource(R.string.data_restore_text, dateTimeText(summary.createdAt), contents)
}

private fun BackupFrequency.labelRes(): Int = when (this) {
    BackupFrequency.OFF -> R.string.data_frequency_off
    BackupFrequency.DAILY -> R.string.data_frequency_daily
    BackupFrequency.WEEKLY -> R.string.data_frequency_weekly
}

private fun DataMessage.textRes(): Int = when (this) {
    DataMessage.BACKED_UP -> R.string.data_backed_up
    DataMessage.BACKUP_FAILED -> R.string.data_backup_failed
    DataMessage.FOLDER_FAILED -> R.string.data_folder_failed
    DataMessage.EXPORTED -> R.string.data_exported
    DataMessage.EXPORT_FAILED -> R.string.data_export_failed
    DataMessage.NOT_A_BACKUP -> R.string.data_not_a_backup
    DataMessage.RESTORED -> R.string.data_restored
    DataMessage.RESTORE_FAILED -> R.string.data_restore_failed
    DataMessage.RESTORE_UNDONE -> R.string.data_restore_undone
}

@Composable
private fun MessageSnackbar(message: DataMessage?, host: SnackbarHostState, actions: DataActions) {
    val current by rememberUpdatedState(actions)
    val text = message?.let { stringResource(it.textRes()) }
    val undo = stringResource(UiR.string.ui_undo)
    LaunchedEffect(message) {
        if (text != null) {
            val canUndo = message == DataMessage.RESTORED
            val result = host.showSnackbar(text, actionLabel = undo.takeIf { canUndo })
            current.onDismissMessage()
            if (result == SnackbarResult.ActionPerformed) current.onUndoRestore()
        }
    }
}

/** What the page can ask for; the system's pickers come first where a document is needed. */
internal class DataActions(
    val onChooseFolder: () -> Unit,
    val onScheduleChange: (BackupFrequency, Int) -> Unit,
    val onBackUpNow: () -> Unit,
    val onExportJson: () -> Unit,
    val onExportCsv: () -> Unit,
    val onPickBackup: () -> Unit,
    val onRestore: () -> Unit,
    val onDismissRestore: () -> Unit,
    val onUndoRestore: () -> Unit,
    val onPickImport: () -> Unit,
    val onToggleImages: () -> Unit,
    val onDismissMessage: () -> Unit,
)
