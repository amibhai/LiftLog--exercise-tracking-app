package io.github.amibhai.liftlog.feature.routines.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.routines.R
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun RoutineEditorRoute(
    navigation: RoutineEditorNavigation,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(navigation.onBack)
    LaunchedEffect(state) { if (state == RoutineEditorUiState.Gone) back() }
    (state as? RoutineEditorUiState.Ready)?.let {
        RoutineEditorScreen(state = it, onAction = viewModel::onAction, navigation = navigation)
    }
}

@Composable
internal fun RoutineEditorScreen(
    state: RoutineEditorUiState.Ready,
    onAction: (RoutineEditorAction) -> Unit,
    navigation: RoutineEditorNavigation,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<RoutineDialog?>(null) }
    var hasAskedForName by rememberSaveable { mutableStateOf(false) }
    val snackbars = remember { SnackbarHostState() }
    // A new routine starts with its name.
    LaunchedEffect(Unit) {
        if (!hasAskedForName && state.name.isBlank() && state.exercises.isEmpty()) dialog = RoutineDialog.Rename
        hasAskedForName = true
    }
    RemovedSnackbar(state.message, snackbars, onAction)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.name.ifBlank { stringResource(R.string.routine_new) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigation.onBack) {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                },
                actions = {
                    IconButton(onClick = { dialog = RoutineDialog.Rename }) {
                        Icon(painterResource(LiftLogIcons.Edit), stringResource(R.string.routine_rename))
                    }
                    if (state.exercises.size > 1) {
                        IconButton(onClick = { dialog = RoutineDialog.Reorder }) {
                            Icon(painterResource(LiftLogIcons.Reorder), stringResource(R.string.routine_reorder))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(LiftLogDimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        ) {
            if (state.exercises.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = LiftLogIcons.Exercises,
                        title = stringResource(R.string.routine_empty_title),
                        body = stringResource(R.string.routine_empty_body),
                    )
                }
            }
            items(state.exercises, key = { it.id.value }) { exercise ->
                RoutineExerciseCard(
                    exercise = exercise,
                    effortMode = state.effortMode,
                    onAction = onAction,
                    onOpenDialog = { dialog = it },
                    onOpenExercise = { navigation.onOpenExercise(exercise.exerciseId) },
                    onReplace = { navigation.onReplaceExercise(state.id, exercise.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "add") {
                val wide = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LiftLogDimens.primaryAction)
                val add = { navigation.onAddExercises(state.id) }
                if (state.exercises.isEmpty()) {
                    Button(onClick = add, modifier = wide) { Text(stringResource(R.string.routine_add_exercises)) }
                } else {
                    FilledTonalButton(
                        onClick = add,
                        modifier = wide,
                    ) { Text(stringResource(R.string.routine_add_exercises)) }
                }
            }
        }
    }
    dialog?.let { RoutineDialogs(dialog = it, state = state, onAction = onAction, onDismiss = { dialog = null }) }
}

@Composable
private fun RemovedSnackbar(
    message: RoutineEditorMessage?,
    host: SnackbarHostState,
    onAction: (RoutineEditorAction) -> Unit,
) {
    val act by rememberUpdatedState(onAction)
    val text = stringResource(R.string.routine_exercise_removed)
    val undo = stringResource(UiR.string.ui_undo)
    LaunchedEffect(message) {
        if (message != null) {
            val result = host.showSnackbar(text, actionLabel = undo)
            val isUndone = result == SnackbarResult.ActionPerformed
            act(if (isUndone) RoutineEditorAction.Undo else RoutineEditorAction.DismissMessage)
        }
    }
}
