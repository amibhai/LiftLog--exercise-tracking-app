package io.github.amibhai.liftlog.feature.exercises.picker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.ui.component.ExerciseThumbnail
import io.github.amibhai.liftlog.core.ui.component.HighlightedText
import io.github.amibhai.liftlog.core.ui.component.SearchField
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.ExerciseListActions
import io.github.amibhai.liftlog.feature.exercises.R
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseList
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseRowDetails
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseRowUiModel
import io.github.amibhai.liftlog.feature.exercises.list.FilterRow
import io.github.amibhai.liftlog.core.ui.R as UiR

private val SelectedMarkSize = 48.dp

/** [createdExercise] is the id of a custom exercise that was just created from here. */
@Composable
internal fun ExercisePickerRoute(
    createdExercise: String?,
    onClose: () -> Unit,
    onOpenExercise: (ExerciseId) -> Unit,
    onCreateExercise: (name: String) -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query = rememberTextFieldState()
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(query) { snapshotFlow { query.text.toString() }.collect(viewModel::onQueryChange) }
    LaunchedEffect(createdExercise) { createdExercise?.let { viewModel.onCreated(ExerciseId(it)) } }
    LaunchedEffect(state.isDone) { if (state.isDone) close() }
    val actions = remember(viewModel) {
        ExercisePickerActions(
            list = ExerciseListActions(
                onMuscleChange = viewModel::onMuscleChange,
                onEquipmentChange = viewModel::onEquipmentChange,
                onOpenExercise = onOpenExercise,
                onCreateExercise = onCreateExercise,
            ),
            onPick = viewModel::onPick,
            onAdd = viewModel::add,
            onClose = { close() },
        )
    }
    ExercisePickerScreen(state = state, query = query, actions = actions)
}

@Composable
internal fun ExercisePickerScreen(
    state: ExercisePickerUiState,
    query: TextFieldState,
    actions: ExercisePickerActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isReplacing) {
                                R.string.exercises_picker_replace_title
                            } else {
                                R.string.exercises_picker_title
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(painterResource(LiftLogIcons.Close), stringResource(UiR.string.ui_close))
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = state.selected.isNotEmpty()) {
                AddBar(count = state.selected.size, onAdd = actions.onAdd)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                state = query,
                hint = stringResource(R.string.exercises_search_hint),
                modifier = Modifier.padding(
                    horizontal = LiftLogDimens.screenGutter,
                    vertical = LiftLogDimens.spacingXs,
                ),
            )
            FilterRow(state.list.filter, actions.list.onMuscleChange, actions.list.onEquipmentChange)
            ExerciseList(
                state = state.list,
                typed = query.text.toString(),
                onCreate = actions.list.onCreateExercise,
                modifier = Modifier.weight(1f),
            ) { row ->
                PickerRow(
                    row = row,
                    isSelected = row.id in state.selected,
                    onClick = { actions.onPick(row.id) },
                    onLongClick = { actions.list.onOpenExercise(row.id) },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    row: ExerciseRowUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = modifier,
        leadingContent = { if (isSelected) SelectedMark() else ExerciseThumbnail(row.thumbnailUrl) },
        supportingContent = { ExerciseRowDetails(row) },
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(R.string.exercises_picker_details),
    ) {
        HighlightedText(row.name, row.highlights)
    }
}

@Composable
private fun SelectedMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(SelectedMarkSize),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Icon(
            painter = painterResource(LiftLogIcons.Check),
            contentDescription = null,
            modifier = Modifier.padding(LiftLogDimens.spacingM),
        )
    }
}

@Composable
private fun AddBar(count: Int, onAdd: (asSuperset: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(LiftLogDimens.spacingM),
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val button = Modifier.weight(1f).heightIn(min = LiftLogDimens.primaryAction)
            if (count > 1) {
                OutlinedButton(onClick = { onAdd(true) }, modifier = button) {
                    Text(stringResource(R.string.exercises_picker_superset))
                }
            }
            Button(onClick = { onAdd(false) }, modifier = button) {
                Text(stringResource(R.string.exercises_picker_add, count))
            }
        }
    }
}
