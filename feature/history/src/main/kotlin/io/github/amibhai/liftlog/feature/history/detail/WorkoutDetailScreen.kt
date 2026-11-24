package io.github.amibhai.liftlog.feature.history.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.ui.component.Celebration
import io.github.amibhai.liftlog.core.ui.component.ConfirmDialog
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.StatItem
import io.github.amibhai.liftlog.core.ui.component.TextInputDialog
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.dateTimeText
import io.github.amibhai.liftlog.core.ui.format.workoutTitle
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.history.R
import io.github.amibhai.liftlog.core.ui.R as UiR

private val SetBadgeWidth = LiftLogDimens.spacingXxl

private enum class DetailDialog { SAVE_ROUTINE, DELETE, ACTIVE_WORKOUT }

@Composable
internal fun WorkoutDetailRoute(
    onBack: () -> Unit,
    onOpenWorkout: (WorkoutId) -> Unit,
    onOpenExercise: (ExerciseId) -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(onBack)
    val open by rememberUpdatedState(onOpenWorkout)
    val started = (state as? WorkoutDetailUiState.Ready)?.openWorkout
    LaunchedEffect(state) { if (state == WorkoutDetailUiState.Gone) back() }
    LaunchedEffect(started) {
        if (started != null) {
            open(started)
            viewModel.onWorkoutOpened()
        }
    }
    val actions = remember(viewModel, onOpenExercise) {
        WorkoutDetailActions(
            onBack = { back() },
            onOpenWorkout = { open(it) },
            onOpenExercise = onOpenExercise,
            onRepeat = viewModel::repeat,
            onSaveAsRoutine = viewModel::saveAsRoutine,
            onUpdateRoutine = viewModel::updateRoutine,
            onDelete = viewModel::delete,
        )
    }
    (state as? WorkoutDetailUiState.Ready)?.let { WorkoutDetailScreen(state = it, actions = actions) }
}

@Composable
internal fun WorkoutDetailScreen(
    state: WorkoutDetailUiState.Ready,
    actions: WorkoutDetailActions,
    modifier: Modifier = Modifier,
) {
    var dialog by rememberSaveable { mutableStateOf<DetailDialog?>(null) }
    val repeat = { if (state.activeWorkoutId != null) dialog = DetailDialog.ACTIVE_WORKOUT else actions.onRepeat() }
    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                state = state,
                actions = actions,
                onRepeat = repeat,
                onDelete = { dialog = DetailDialog.DELETE },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(LiftLogDimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        ) {
            if (state.justFinished) item(key = "finished") { FinishedCard() }
            if (state.records.isNotEmpty()) item(key = "records") { RecordsCard(state, actions.onOpenExercise) }
            item(key = "summary") { SummaryCard(state) }
            val notes = state.notes
            if (notes != null) item(key = "notes") { Text(notes, style = MaterialTheme.typography.bodyLarge) }
            if (state.muscles.isNotEmpty()) item(key = "muscles") { MusclesCard(state) }
            items(state.exercises, key = { it.id.value }) { exercise ->
                ExerciseCard(exercise, onOpen = { actions.onOpenExercise(exercise.exerciseId) })
            }
            if (state.routineDone != null || state.routine != RoutineOffer.None) {
                item(key = "routine") {
                    RoutineCard(
                        state = state,
                        onSave = { dialog = DetailDialog.SAVE_ROUTINE },
                        onUpdate = actions.onUpdateRoutine,
                    )
                }
            }
            if (state.justFinished) {
                item(key = "done") {
                    Button(
                        onClick = actions.onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = LiftLogDimens.primaryAction),
                    ) { Text(stringResource(R.string.history_done)) }
                }
            }
        }
    }
    if (state.justFinished && state.records.isNotEmpty()) CelebrateRecords()
    dialog?.let { DetailDialogs(it, state, actions, onDismiss = { dialog = null }) }
}

/** A burst of confetti and a haptic, once, when a workout just finished broke records (SPEC §13). */
@Composable
private fun CelebrateRecords() {
    val haptics = LocalHapticFeedback.current
    var isOver by rememberSaveable { mutableStateOf(false) }
    if (isOver) return
    LaunchedEffect(Unit) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    Celebration(onEnd = { isOver = true }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun DetailTopBar(
    state: WorkoutDetailUiState.Ready,
    actions: WorkoutDetailActions,
    onRepeat: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        modifier = modifier,
        title = { Text(workoutTitle(state.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
            }
        },
        actions = {
            IconButton(onClick = { actions.onOpenWorkout(state.id) }) {
                Icon(painterResource(LiftLogIcons.Edit), stringResource(R.string.history_edit))
            }
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(painterResource(LiftLogIcons.More), stringResource(UiR.string.ui_more_options))
                }
                DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_repeat)) },
                        onClick = {
                            isMenuOpen = false
                            onRepeat()
                        },
                        leadingIcon = { Icon(painterResource(LiftLogIcons.Repeat), contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_delete)) },
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        },
                        leadingIcon = { Icon(painterResource(LiftLogIcons.Delete), contentDescription = null) },
                    )
                }
            }
        },
    )
}

@Composable
private fun FinishedCard(modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.history_finished_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.history_finished_body), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SummaryCard(state: WorkoutDetailUiState.Ready, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        Text(dateTimeText(state.startedAt), style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            StatItem(stringResource(R.string.history_stat_time), LiftLogFormat.duration(state.durationSeconds))
            StatItem(stringResource(R.string.history_stat_volume), state.volume)
            StatItem(stringResource(R.string.history_stat_sets), state.sets.toString())
        }
    }
}

@Composable
private fun ExerciseCard(exercise: DetailExercise, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingXs) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            exercise.supersetLetter?.let {
                Text(
                    text = stringResource(R.string.history_superset, it.toString()),
                    modifier = Modifier.padding(end = LiftLogDimens.spacingS),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = exercise.name,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = LiftLogDimens.minTouchTarget)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(vertical = LiftLogDimens.spacingM)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        exercise.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        for (set in exercise.sets) SetLine(set)
    }
}

@Composable
private fun SetLine(set: DetailSet, modifier: Modifier = Modifier) {
    val (badge, kind) = when (set.type) {
        SetType.WARMUP -> stringResource(UiR.string.ui_set_badge_warmup) to stringResource(UiR.string.ui_set_warmup)
        SetType.WORKING -> set.number.toString() to stringResource(UiR.string.ui_set_working, set.number ?: 0)
        SetType.DROP -> stringResource(UiR.string.ui_set_badge_drop) to stringResource(UiR.string.ui_set_drop)
        SetType.FAILURE -> stringResource(UiR.string.ui_set_badge_failure) to stringResource(UiR.string.ui_set_failure)
    }
    val description = listOfNotNull(kind, set.text, set.effort).joinToString()
    val colors = MaterialTheme.colorScheme
    val badgeColor = if (set.type == SetType.WORKING) colors.onSurface else colors.tertiary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = badge,
            modifier = Modifier.widthIn(min = SetBadgeWidth),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            textAlign = TextAlign.Center,
        )
        Text(set.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        set.effort?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoutineCard(
    state: WorkoutDetailUiState.Ready,
    onSave: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        val done = state.routineDone
        val offer = state.routine
        when {
            done is RoutineDone.Saved -> Text(stringResource(R.string.history_routine_saved, done.name))

            done is RoutineDone.Updated -> Text(stringResource(R.string.history_routine_updated, done.name))

            offer is RoutineOffer.Update -> {
                Text(
                    text = stringResource(R.string.history_update_routine_title, offer.name),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.history_update_routine_body), style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(
                    onClick = onUpdate,
                    modifier = Modifier.heightIn(min = LiftLogDimens.minTouchTarget),
                ) {
                    Text(stringResource(R.string.history_update_routine))
                }
            }

            else -> {
                Text(
                    text = stringResource(R.string.history_save_routine),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.history_save_routine_body), style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = onSave, modifier = Modifier.heightIn(min = LiftLogDimens.minTouchTarget)) {
                    Text(stringResource(R.string.history_save_routine))
                }
            }
        }
    }
}

@Composable
private fun DetailDialogs(
    dialog: DetailDialog,
    state: WorkoutDetailUiState.Ready,
    actions: WorkoutDetailActions,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        DetailDialog.SAVE_ROUTINE -> TextInputDialog(
            title = stringResource(R.string.history_routine_name),
            initial = workoutTitle(state.title),
            onConfirm = {
                onDismiss()
                actions.onSaveAsRoutine(it)
            },
            onDismiss = onDismiss,
        )

        DetailDialog.DELETE -> ConfirmDialog(
            title = stringResource(R.string.history_delete_title),
            text = stringResource(R.string.history_delete_text),
            confirmLabel = stringResource(UiR.string.ui_delete),
            onConfirm = {
                onDismiss()
                actions.onDelete()
            },
            onDismiss = onDismiss,
            isDestructive = true,
        )

        DetailDialog.ACTIVE_WORKOUT -> ConfirmDialog(
            title = stringResource(R.string.history_active_title),
            text = stringResource(R.string.history_active_text),
            confirmLabel = stringResource(R.string.history_resume),
            onConfirm = {
                onDismiss()
                state.activeWorkoutId?.let(actions.onOpenWorkout)
            },
            onDismiss = onDismiss,
        )
    }
}
