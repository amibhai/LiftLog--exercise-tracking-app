package io.github.amibhai.liftlog.feature.routines.programs.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.ui.component.ConfirmDialog
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.OptionsDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderItem
import io.github.amibhai.liftlog.core.ui.component.SwitchRow
import io.github.amibhai.liftlog.core.ui.component.TextInputDialog
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.routines.R
import io.github.amibhai.liftlog.core.ui.R as UiR

/** The dialog that is open, if any. */
private sealed interface ProgramDialog {
    data object Rename : ProgramDialog

    data object Reorder : ProgramDialog

    data object AddDay : ProgramDialog

    data object Delete : ProgramDialog

    data class RenameDay(val day: ProgramDayUiModel) : ProgramDialog
}

@Composable
internal fun ProgramRoute(
    onBack: () -> Unit,
    onEditRoutine: (RoutineId) -> Unit,
    viewModel: ProgramViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(onBack)
    LaunchedEffect(state) { if (state == ProgramUiState.Gone) back() }
    (state as? ProgramUiState.Ready)?.let {
        ProgramScreen(state = it, onAction = viewModel::onAction, onBack = onBack, onEditRoutine = onEditRoutine)
    }
}

@Composable
internal fun ProgramScreen(
    state: ProgramUiState.Ready,
    onAction: (ProgramAction) -> Unit,
    onBack: () -> Unit,
    onEditRoutine: (RoutineId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<ProgramDialog?>(null) }
    var hasAskedForName by rememberSaveable { mutableStateOf(false) }
    val snackbars = remember { SnackbarHostState() }
    // A new program starts with its name.
    LaunchedEffect(Unit) {
        if (!hasAskedForName && state.name.isBlank() && state.days.isEmpty()) dialog = ProgramDialog.Rename
        hasAskedForName = true
    }
    RemovedDaySnackbar(state.canUndo, snackbars, onAction)
    Scaffold(
        modifier = modifier,
        topBar = { ProgramTopBar(state = state, onBack = onBack, onOpenDialog = { dialog = it }) },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = LiftLogDimens.spacingS),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        ) {
            item(key = "follow") {
                SwitchRow(
                    title = stringResource(R.string.programs_follow),
                    checked = state.isActive,
                    onCheckedChange = { onAction(ProgramAction.Follow(it)) },
                    supportingText = stringResource(R.string.programs_follow_body),
                )
            }
            items(state.days, key = { it.id.value }) { day ->
                DayCard(
                    day = day,
                    onOpen = { day.routineId?.let(onEditRoutine) },
                    onRename = { dialog = ProgramDialog.RenameDay(day) },
                    onRemove = { onAction(ProgramAction.RemoveDay(day.id)) },
                    modifier = Modifier
                        .padding(horizontal = LiftLogDimens.screenGutter)
                        .animateItem(),
                )
            }
            item(key = "add") { AddDay(state, onAdd = { dialog = ProgramDialog.AddDay }) }
        }
    }
    dialog?.let { ProgramDialogs(it, state, onAction, onDismiss = { dialog = null }) }
}

@Composable
private fun ProgramTopBar(
    state: ProgramUiState.Ready,
    onBack: () -> Unit,
    onOpenDialog: (ProgramDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = state.name.ifBlank { stringResource(R.string.programs_untitled) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
            }
        },
        actions = {
            IconButton(onClick = { onOpenDialog(ProgramDialog.Rename) }) {
                Icon(painterResource(LiftLogIcons.Edit), stringResource(R.string.programs_rename))
            }
            if (state.days.size > 1) {
                IconButton(onClick = { onOpenDialog(ProgramDialog.Reorder) }) {
                    Icon(painterResource(LiftLogIcons.Reorder), stringResource(R.string.programs_reorder))
                }
            }
            IconButton(onClick = { onOpenDialog(ProgramDialog.Delete) }) {
                Icon(painterResource(LiftLogIcons.Delete), stringResource(R.string.programs_delete))
            }
        },
    )
}

@Composable
private fun DayCard(
    day: ProgramDayUiModel,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiftLogCard(onClick = onOpen, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(day.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (day.isNext) {
                Text(
                    text = stringResource(R.string.programs_next),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DayMenu(day.name, onRename, onRemove)
        }
        Text(
            text = if (day.routineId == null) {
                stringResource(R.string.programs_no_routine)
            } else {
                day.exercises.joinToString()
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayMenu(name: String, onRename: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    var isOpen by remember { mutableStateOf(false) }
    val choose: (() -> Unit) -> Unit = { action ->
        isOpen = false
        action()
    }
    Box(modifier) {
        IconButton(onClick = { isOpen = true }) {
            Icon(painterResource(LiftLogIcons.More), stringResource(R.string.programs_day_options, name))
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.programs_rename_day)) },
                onClick = { choose(onRename) },
                leadingIcon = { Icon(painterResource(LiftLogIcons.Edit), contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.programs_remove_day)) },
                onClick = { choose(onRemove) },
                leadingIcon = { Icon(painterResource(LiftLogIcons.Delete), contentDescription = null) },
            )
        }
    }
}

@Composable
private fun AddDay(state: ProgramUiState.Ready, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    if (state.routines.isEmpty()) {
        Text(
            text = stringResource(R.string.programs_no_routines),
            modifier = modifier.padding(horizontal = LiftLogDimens.screenGutter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        FilledTonalButton(
            onClick = onAdd,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = LiftLogDimens.screenGutter)
                .heightIn(min = LiftLogDimens.primaryAction),
        ) { Text(stringResource(R.string.programs_add_day)) }
    }
}

@Composable
private fun ProgramDialogs(
    dialog: ProgramDialog,
    state: ProgramUiState.Ready,
    onAction: (ProgramAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val act: (ProgramAction) -> Unit = {
        onDismiss()
        onAction(it)
    }
    when (dialog) {
        ProgramDialog.Rename -> TextInputDialog(
            title = stringResource(R.string.programs_name),
            initial = state.name,
            onConfirm = { act(ProgramAction.Rename(it)) },
            onDismiss = onDismiss,
        )

        ProgramDialog.Reorder -> ReorderDialog(
            title = stringResource(R.string.programs_reorder),
            items = state.days.map { ReorderItem(it.id.value, it.name, null) },
            onConfirm = { ids -> act(ProgramAction.Reorder(ids.map(::ProgramDayId))) },
            onDismiss = onDismiss,
        )

        ProgramDialog.AddDay -> OptionsDialog(
            title = stringResource(R.string.programs_add_day),
            options = state.routines,
            selected = null,
            label = { it?.name.orEmpty().ifBlank { stringResource(R.string.programs_untitled_routine) } },
            onSelect = { routine -> routine?.let { act(ProgramAction.AddDay(it)) } },
            onDismiss = onDismiss,
        )

        ProgramDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.programs_delete_title),
            text = stringResource(R.string.programs_delete_text),
            confirmLabel = stringResource(R.string.programs_delete),
            onConfirm = { act(ProgramAction.Delete) },
            onDismiss = onDismiss,
            isDestructive = true,
        )

        is ProgramDialog.RenameDay -> TextInputDialog(
            title = stringResource(R.string.programs_day_name),
            initial = dialog.day.name,
            onConfirm = { act(ProgramAction.RenameDay(dialog.day.id, it)) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun RemovedDaySnackbar(canUndo: Boolean, host: SnackbarHostState, onAction: (ProgramAction) -> Unit) {
    val act by rememberUpdatedState(onAction)
    val text = stringResource(R.string.programs_day_removed)
    val undo = stringResource(UiR.string.ui_undo)
    LaunchedEffect(canUndo) {
        if (canUndo) {
            val result = host.showSnackbar(text, actionLabel = undo)
            act(if (result == SnackbarResult.ActionPerformed) ProgramAction.Undo else ProgramAction.DismissMessage)
        }
    }
}
