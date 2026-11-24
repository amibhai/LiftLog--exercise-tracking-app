package io.github.amibhai.liftlog.feature.workout.logging

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.ui.component.EffortChips
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.component.SetRow
import io.github.amibhai.liftlog.core.ui.component.SetRowAction
import io.github.amibhai.liftlog.core.ui.component.SetRowHeader
import io.github.amibhai.liftlog.core.ui.component.SwipeActions
import io.github.amibhai.liftlog.core.ui.format.dateTimeText
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.workout.R

private val HintIconSize = 18.dp

@Composable
internal fun LazyItemScope.WorkoutListItem(
    item: WorkoutItem,
    onAction: (WorkoutAction) -> Unit,
    navigation: WorkoutNavigation,
    onOpenDialog: (WorkoutDialog) -> Unit,
    workoutId: WorkoutId,
) {
    when (item) {
        is WorkoutItem.Header -> ExerciseHeader(
            header = item,
            onOpen = { navigation.onOpenExercise(item.exerciseId) },
            onReplace = { navigation.onReplaceExercise(workoutId, item.id) },
            onAction = onAction,
            onOpenDialog = onOpenDialog,
        )

        is WorkoutItem.Set -> SwipeActions(
            onDuplicate = { onAction(WorkoutAction.Row(SetRowAction.Duplicate(item.row.id))) },
            onDelete = { onAction(WorkoutAction.Row(SetRowAction.Delete(item.row.id))) },
            modifier = Modifier.animateItem(),
        ) {
            SetRow(
                model = item.row,
                onAction = { onAction(WorkoutAction.Row(it)) },
                modifier = Modifier.padding(horizontal = LiftLogDimens.spacingS),
            )
        }

        is WorkoutItem.Effort -> EffortChips(
            mode = item.mode,
            selected = item.selected,
            onSelect = { onAction(WorkoutAction.Rate(item.setId, it)) },
            modifier = Modifier.animateItem(),
        )

        is WorkoutItem.AddSet -> TextButton(
            onClick = { onAction(WorkoutAction.AddSet(item.id)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LiftLogDimens.minTouchTarget)
                .padding(horizontal = LiftLogDimens.screenGutter)
                .animateItem(),
        ) {
            Icon(painterResource(LiftLogIcons.Add), contentDescription = null)
            Spacer(Modifier.width(LiftLogDimens.spacingS))
            Text(stringResource(R.string.workout_add_set))
        }
    }
}

@Composable
private fun ExerciseHeader(
    header: WorkoutItem.Header,
    onOpen: () -> Unit,
    onReplace: () -> Unit,
    onAction: (WorkoutAction) -> Unit,
    onOpenDialog: (WorkoutDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(top = LiftLogDimens.spacingL)) {
        Row(
            modifier = Modifier.padding(start = LiftLogDimens.screenGutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            header.supersetLetter?.let { SupersetMark(it) }
            Text(
                text = header.name,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .padding(vertical = LiftLogDimens.spacingM)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ExerciseMenu(header, onReplace, onAction, onOpenDialog)
        }
        header.stickyNote?.let { note ->
            NoteText(note, onClick = { onOpenDialog(WorkoutDialog.StickyNote(header)) }, icon = LiftLogIcons.Pin)
        }
        header.notes?.let { NoteText(it, onClick = { onOpenDialog(WorkoutDialog.ExerciseNotes(header)) }) }
        header.reason?.let { suggestionText(it, header.unit) }?.let { HintText(it) }
        SetRowHeader(
            setTitle = stringResource(R.string.workout_column_set),
            previousTitle = stringResource(R.string.workout_column_previous),
            fieldTitles = header.columns.map { columnTitle(it) },
            modifier = Modifier.padding(horizontal = LiftLogDimens.spacingS),
        )
    }
}

/** Why the sets below are planned as they are. */
@Composable
private fun HintText(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(LiftLogIcons.Progress),
            contentDescription = null,
            modifier = Modifier.size(HintIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SupersetMark(letter: Char, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.workout_superset, letter.toString())
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = description,
            modifier = Modifier.padding(horizontal = LiftLogDimens.spacingS, vertical = LiftLogDimens.spacingXs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ExerciseMenu(
    header: WorkoutItem.Header,
    onReplace: () -> Unit,
    onAction: (WorkoutAction) -> Unit,
    onOpenDialog: (WorkoutDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    val open: (WorkoutDialog) -> Unit = {
        isOpen = false
        onOpenDialog(it)
    }
    Box(modifier) {
        IconButton(onClick = { isOpen = true }) {
            Icon(painterResource(LiftLogIcons.More), stringResource(R.string.workout_exercise_options, header.name))
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            MenuItem(stringResource(R.string.workout_exercise_note), LiftLogIcons.Note) {
                open(WorkoutDialog.ExerciseNotes(header))
            }
            MenuItem(stringResource(R.string.workout_exercise_pinned_note), LiftLogIcons.Pin) {
                open(WorkoutDialog.StickyNote(header))
            }
            MenuItem(stringResource(R.string.workout_exercise_rest), LiftLogIcons.Timer) {
                open(WorkoutDialog.Rest(header))
            }
            if (header.canWarmUp) {
                MenuItem(stringResource(R.string.workout_exercise_warmups), LiftLogIcons.Warmup) {
                    isOpen = false
                    onAction(WorkoutAction.AddWarmups(header.id))
                }
            }
            MenuItem(stringResource(R.string.workout_exercise_replace), LiftLogIcons.Replace) {
                isOpen = false
                onReplace()
            }
            if (header.supersetLetter == null) {
                MenuItem(stringResource(R.string.workout_exercise_superset), LiftLogIcons.Link) {
                    open(WorkoutDialog.Superset(header))
                }
            } else {
                MenuItem(stringResource(R.string.workout_exercise_leave_superset), LiftLogIcons.LinkOff) {
                    isOpen = false
                    onAction(WorkoutAction.LeaveSuperset(header.id))
                }
            }
            MenuItem(stringResource(R.string.workout_exercise_remove), LiftLogIcons.Delete) {
                isOpen = false
                onAction(WorkoutAction.RemoveExercise(header.id))
            }
        }
    }
}

@Composable
private fun columnTitle(column: ColumnUiModel): String = when (column.field) {
    SetField.WEIGHT, SetField.DISTANCE -> column.unit.orEmpty()
    SetField.REPS, SetField.DURATION -> stringResource(column.field.labelRes())
}

@Composable
internal fun NoteText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = LiftLogIcons.Note,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Start and end of an earlier workout, which can be corrected. */
@Composable
internal fun WorkoutTimes(
    state: WorkoutUiState.Ready,
    onOpenDialog: (WorkoutDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TimeRow(stringResource(R.string.workout_started), state.startedAt) { onOpenDialog(WorkoutDialog.StartTime) }
        TimeRow(stringResource(R.string.workout_ended), state.endedAt ?: state.startedAt) {
            onOpenDialog(WorkoutDialog.EndTime)
        }
    }
}

@Composable
private fun TimeRow(title: String, at: Long, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        supportingContent = { Text(dateTimeText(at)) },
        leadingContent = { Icon(painterResource(LiftLogIcons.Schedule), contentDescription = null) },
        trailingContent = { Icon(painterResource(LiftLogIcons.Edit), contentDescription = null) },
    ) { Text(title) }
}

@Composable
internal fun WorkoutFooter(
    state: WorkoutUiState.Ready,
    onAddExercises: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(LiftLogDimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
    ) {
        val wide = Modifier.fillMaxWidth().heightIn(min = LiftLogDimens.primaryAction)
        if (state.exercises.isEmpty()) {
            EmptyState(
                icon = LiftLogIcons.Exercises,
                title = stringResource(R.string.workout_empty_title),
                body = stringResource(R.string.workout_empty_body),
            )
            Button(onClick = onAddExercises, modifier = wide) { Text(stringResource(R.string.workout_add_exercises)) }
        } else {
            FilledTonalButton(onClick = onAddExercises, modifier = wide) {
                Text(stringResource(R.string.workout_add_exercises))
            }
            if (state.isInProgress) {
                OutlinedButton(onClick = onFinish, modifier = wide) {
                    Text(stringResource(R.string.workout_finish_long))
                }
            }
        }
    }
}
