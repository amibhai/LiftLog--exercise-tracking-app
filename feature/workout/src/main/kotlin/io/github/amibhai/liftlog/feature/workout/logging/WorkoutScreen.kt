package io.github.amibhai.liftlog.feature.workout.logging

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.ui.component.NumberPadKey
import io.github.amibhai.liftlog.core.ui.list.animateItemIntoView
import io.github.amibhai.liftlog.feature.workout.R
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun WorkoutRoute(
    navigation: WorkoutNavigation,
    onFinish: (WorkoutId) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(navigation.onClose)
    val finish by rememberUpdatedState(onFinish)
    val ready = state as? WorkoutUiState.Ready
    val finishedId = ready?.takeIf { it.isFinished }?.id
    LaunchedEffect(state) { if (state == WorkoutUiState.Gone) close() }
    LaunchedEffect(finishedId) { finishedId?.let(finish) }
    if (ready?.isInProgress == true) RequestNotificationPermission()
    if (ready != null) WorkoutScreen(state = ready, onAction = viewModel::onAction, navigation = navigation)
}

@Composable
internal fun WorkoutScreen(
    state: WorkoutUiState.Ready,
    onAction: (WorkoutAction) -> Unit,
    navigation: WorkoutNavigation,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<WorkoutDialog?>(null) }
    val snackbars = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val openDialog: (WorkoutDialog) -> Unit = { dialog = it }

    if (state.keepScreenOn) KeepScreenOn()
    BackHandler(enabled = state.pad != null) { onAction(WorkoutAction.PadKey(NumberPadKey.Hide)) }
    UndoSnackbar(state.message, snackbars, onAction)

    Scaffold(
        modifier = modifier,
        topBar = { WorkoutTopBar(state = state, onClose = navigation.onClose, onOpenDialog = openDialog) },
        snackbarHost = { SnackbarHost(snackbars) },
        bottomBar = { BottomControls(state = state, onAction = onAction) },
    ) { padding ->
        val leading = state.leadingItems
        LaunchedEffect(state.focus, padding.calculateBottomPadding()) {
            val index = state.items.indexOfFirst { it.key == state.focus?.setId }
            if (index >= 0) listState.animateItemIntoView(index + leading)
        }
        LazyColumn(state = listState, contentPadding = padding) {
            leadingItems(state, openDialog)
            items(state.items, key = { it.key }, contentType = { it::class }) { item ->
                WorkoutListItem(item, onAction, navigation, openDialog, workoutId = state.id)
            }
            item(key = "footer") {
                WorkoutFooter(
                    state = state,
                    onAddExercises = { navigation.onAddExercises(state.id) },
                    onFinish = { openDialog(finishDialog(state)) },
                )
            }
        }
    }
    dialog?.let { WorkoutDialogs(it, state, onAction, onDismiss = { dialog = null }) }
}

/** How many items [leadingItems] puts before the exercises. */
private val WorkoutUiState.Ready.leadingItems: Int
    get() = (if (isInProgress) 0 else 1) + (if (notes == null) 0 else 1)

private fun LazyListScope.leadingItems(state: WorkoutUiState.Ready, onOpenDialog: (WorkoutDialog) -> Unit) {
    if (!state.isInProgress) {
        item(key = "times") { WorkoutTimes(state, onOpenDialog) }
    }
    state.notes?.let { notes ->
        item(key = "notes") { NoteText(notes, onClick = { onOpenDialog(WorkoutDialog.Notes) }) }
    }
}

@Composable
private fun UndoSnackbar(message: WorkoutMessage?, host: SnackbarHostState, onAction: (WorkoutAction) -> Unit) {
    val act by rememberUpdatedState(onAction)
    val text = message?.let {
        stringResource(
            when (it) {
                WorkoutMessage.SET_DELETED -> R.string.workout_set_deleted
                WorkoutMessage.EXERCISE_REMOVED -> R.string.workout_exercise_removed
            },
        )
    }
    val undo = stringResource(UiR.string.ui_undo)
    LaunchedEffect(message) {
        if (text != null) {
            val result = host.showSnackbar(text, actionLabel = undo)
            act(if (result == SnackbarResult.ActionPerformed) WorkoutAction.Undo else WorkoutAction.DismissMessage)
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
