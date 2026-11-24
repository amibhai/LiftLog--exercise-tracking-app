package io.github.amibhai.liftlog.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.usecase.PlateauInsight
import io.github.amibhai.liftlog.core.ui.component.ConfirmDialog
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.RecordRow
import io.github.amibhai.liftlog.core.ui.component.WeekCard
import io.github.amibhai.liftlog.core.ui.format.relativeDays
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun HomeRoute(navigation: HomeNavigation, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.openWorkout) {
        state.openWorkout?.let {
            navigation.onOpenWorkout(it)
            viewModel.onWorkoutOpened()
        }
    }
    LaunchedEffect(state.openRoutine) {
        state.openRoutine?.let {
            navigation.onEditRoutine(it)
            viewModel.onRoutineOpened()
        }
    }
    val actions = remember(viewModel) {
        HomeActions(
            onStartEmpty = viewModel::startEmpty,
            onRepeatLast = viewModel::repeatLast,
            onStartRoutine = viewModel::startRoutine,
            onNewRoutine = viewModel::newRoutine,
            onDeleteRoutine = viewModel::deleteRoutine,
            onUndoDeleteRoutine = viewModel::undoDeleteRoutine,
            onDeleteMessageShown = viewModel::onDeleteMessageShown,
            onDismissInsight = viewModel::dismissInsight,
        )
    }
    HomeScreen(state = state, navigation = navigation, actions = actions)
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    navigation: HomeNavigation,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    var isAskingToResume by rememberSaveable { mutableStateOf(false) }
    val snackbars = remember { SnackbarHostState() }
    // There is one workout in progress at a time (SPEC §5).
    val start: (() -> Unit) -> Unit = { begin ->
        if (state.activeWorkoutId != null) isAskingToResume = true else begin()
    }
    DeletedRoutineSnackbar(state.deletedRoutine, snackbars, actions)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = navigation.onOpenSettings) {
                        Icon(painterResource(LiftLogIcons.Settings), stringResource(R.string.home_open_settings))
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
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
        ) {
            item(key = "start") {
                StartCard(
                    state = state,
                    onStartNext = { id -> start { actions.onStartRoutine(id) } },
                    onRepeatLast = { start(actions.onRepeatLast) },
                    onStartEmpty = { start(actions.onStartEmpty) },
                )
            }
            item(key = "routines") {
                RoutinesHeader(onNewRoutine = actions.onNewRoutine, onOpenPrograms = navigation.onOpenPrograms)
            }
            val routines = state.routines.orEmpty()
            if (state.routines != null && routines.isEmpty()) {
                item(key = "no-routines") {
                    Text(
                        text = stringResource(R.string.home_routines_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(routines, key = { it.id.value }) { routine ->
                RoutineCard(
                    routine = routine,
                    onStart = { start { actions.onStartRoutine(routine.id) } },
                    onEdit = { navigation.onEditRoutine(routine.id) },
                    onDelete = { actions.onDeleteRoutine(routine.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            state.training?.let { training ->
                item(key = "week") {
                    WeekCard(training.week, training.streakWeeks, training.unit, onClick = navigation.onOpenProgress)
                }
                if (training.records.isNotEmpty()) {
                    item(key = "records") { RecordsCard(training, onOpenExercise = navigation.onOpenExercise) }
                }
            }
            items(state.insights, key = { it.key }) { insight ->
                InsightCard(
                    insight = insight,
                    onShow = { navigation.onOpenExercise(insight.exerciseId) },
                    onDismiss = { actions.onDismissInsight(insight) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "library") { LibraryCard(state.exerciseCount, navigation.onBrowseExercises) }
        }
    }
    if (isAskingToResume) {
        ConfirmDialog(
            title = stringResource(R.string.home_active_title),
            text = stringResource(R.string.home_active_text),
            confirmLabel = stringResource(R.string.home_resume),
            onConfirm = {
                isAskingToResume = false
                state.activeWorkoutId?.let(navigation.onOpenWorkout)
            },
            onDismiss = { isAskingToResume = false },
        )
    }
}

/** The most likely way to start comes first and stands out. */
@Composable
private fun StartCard(
    state: HomeUiState,
    onStartNext: (RoutineId) -> Unit,
    onRepeatLast: () -> Unit,
    onStartEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = state.next
    val repeatLast = state.repeatLast
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        Text(stringResource(R.string.home_start_title), style = MaterialTheme.typography.titleLarge)
        if (next != null) {
            NextUp(next)
            StartButton(
                text = stringResource(R.string.home_start_next, next.dayName),
                isPrimary = true,
                onClick = { onStartNext(next.routineId) },
            )
        }
        if (repeatLast != null) {
            StartButton(
                text = repeatLast.routineName?.let { stringResource(R.string.home_repeat_routine, it) }
                    ?: stringResource(R.string.home_repeat_workout),
                isPrimary = next == null,
                onClick = onRepeatLast,
            )
        }
        StartButton(
            text = stringResource(R.string.home_start_empty),
            isPrimary = next == null && repeatLast == null,
            onClick = onStartEmpty,
        )
    }
}

@Composable
private fun NextUp(next: NextWorkout, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.home_next_up, next.dayName, next.programName),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = next.exercises.joinToString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StartButton(text: String, isPrimary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val wide = modifier
        .fillMaxWidth()
        .heightIn(min = LiftLogDimens.primaryAction)
    if (isPrimary) {
        Button(onClick = onClick, modifier = wide) { Text(text) }
    } else {
        FilledTonalButton(onClick = onClick, modifier = wide) { Text(text) }
    }
}

@Composable
private fun RoutinesHeader(onNewRoutine: () -> Unit, onOpenPrograms: () -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_routines_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        FlowRow {
            TextButton(onClick = onOpenPrograms, modifier = Modifier.heightIn(min = LiftLogDimens.minTouchTarget)) {
                Text(stringResource(R.string.home_programs))
            }
            TextButton(onClick = onNewRoutine, modifier = Modifier.heightIn(min = LiftLogDimens.minTouchTarget)) {
                Icon(painterResource(LiftLogIcons.Add), contentDescription = null)
                Text(stringResource(R.string.home_new_routine), Modifier.padding(start = LiftLogDimens.spacingS))
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineCardUiModel,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val name = routine.name.ifBlank { stringResource(R.string.home_routine_untitled) }
    LiftLogCard(onClick = onStart, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(painterResource(LiftLogIcons.More), stringResource(R.string.home_routine_options, name))
                }
                DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_routine_edit)) },
                        onClick = {
                            isMenuOpen = false
                            onEdit()
                        },
                        leadingIcon = { Icon(painterResource(LiftLogIcons.Edit), contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_routine_delete)) },
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        },
                        leadingIcon = { Icon(painterResource(LiftLogIcons.Delete), contentDescription = null) },
                    )
                }
            }
        }
        if (routine.exercises.isNotEmpty()) {
            Text(
                text = routine.exercises.joinToString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = routine.lastDoneDaysAgo?.let { stringResource(R.string.home_routine_last, relativeDays(it)) }
                ?: stringResource(R.string.home_routine_never),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordsCard(training: HomeTraining, onOpenExercise: (ExerciseId) -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingXs) {
        Text(
            text = stringResource(R.string.home_records_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        for (entry in training.records) {
            RecordRow(
                entry = entry,
                unit = training.unit,
                withDate = true,
                onClick = { onOpenExercise(entry.record.exerciseId) },
            )
        }
    }
}

/** A lift that stopped moving, and what may help (SPEC §7). */
@Composable
private fun InsightCard(
    insight: PlateauInsight,
    onShow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiftLogCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM)) {
            Icon(
                painter = painterResource(LiftLogIcons.Insight),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.home_plateau,
                    insight.weeks,
                    insight.exerciseName,
                    insight.weeks,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        FlowRow {
            TextButton(onClick = onShow, modifier = Modifier.heightIn(min = LiftLogDimens.minTouchTarget)) {
                Text(stringResource(R.string.home_insight_show))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = LiftLogDimens.minTouchTarget)) {
                Text(stringResource(R.string.home_insight_dismiss))
            }
        }
    }
}

@Composable
private fun LibraryCard(exerciseCount: Int?, onBrowseExercises: () -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        Text(stringResource(R.string.home_library_title), style = MaterialTheme.typography.titleLarge)
        if (exerciseCount != null) {
            Text(
                text = pluralStringResource(R.plurals.home_library_count, exerciseCount, exerciseCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = onBrowseExercises,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LiftLogDimens.minTouchTarget),
        ) {
            Text(stringResource(R.string.home_library_browse))
        }
    }
}

@Composable
private fun DeletedRoutineSnackbar(deleted: RoutineId?, host: SnackbarHostState, actions: HomeActions) {
    val text = stringResource(R.string.home_routine_deleted)
    val undo = stringResource(UiR.string.ui_undo)
    val current by rememberUpdatedState(actions)
    LaunchedEffect(deleted) {
        if (deleted != null) {
            val result = host.showSnackbar(text, actionLabel = undo)
            if (result == SnackbarResult.ActionPerformed) {
                current.onUndoDeleteRoutine()
            } else {
                current.onDeleteMessageShown()
            }
        }
    }
}
