package io.github.amibhai.liftlog.feature.workout.logging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.ui.component.ConfirmDialog
import io.github.amibhai.liftlog.core.ui.component.DateTimeDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderItem
import io.github.amibhai.liftlog.core.ui.component.RestTimeDialog
import io.github.amibhai.liftlog.core.ui.component.TextInputDialog
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.workout.R
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun WorkoutDialogs(
    dialog: WorkoutDialog,
    state: WorkoutUiState.Ready,
    onAction: (WorkoutAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val act: (WorkoutAction) -> Unit = {
        onDismiss()
        onAction(it)
    }
    when (dialog) {
        WorkoutDialog.Rename -> TextInputDialog(
            title = stringResource(R.string.workout_name_label),
            initial = state.title.name.orEmpty(),
            onConfirm = { act(WorkoutAction.Rename(it)) },
            onDismiss = onDismiss,
        )

        WorkoutDialog.Notes -> TextInputDialog(
            title = stringResource(R.string.workout_notes),
            initial = state.notes.orEmpty(),
            onConfirm = { act(WorkoutAction.SetWorkoutNotes(it)) },
            onDismiss = onDismiss,
            singleLine = false,
        )

        WorkoutDialog.Reorder -> ReorderDialog(
            title = stringResource(R.string.workout_reorder),
            items = state.exercises.map { ReorderItem(it.id.value, it.name, it.supersetLetter?.toString()) },
            onConfirm = { ids -> act(WorkoutAction.Reorder(ids.map(::WorkoutExerciseId))) },
            onDismiss = onDismiss,
        )

        WorkoutDialog.StartTime -> DateTimeDialog(
            initial = state.startedAt,
            onConfirm = { act(WorkoutAction.SetTimes(it, (state.endedAt ?: it) + (it - state.startedAt))) },
            onDismiss = onDismiss,
        )

        WorkoutDialog.EndTime -> DateTimeDialog(
            initial = state.endedAt ?: state.startedAt,
            onConfirm = { act(WorkoutAction.SetTimes(state.startedAt, it)) },
            onDismiss = onDismiss,
        )

        WorkoutDialog.Finish, WorkoutDialog.NothingLogged, WorkoutDialog.Discard ->
            EndDialogs(dialog, state, act, onDismiss)

        is WorkoutDialog.ExerciseNotes, is WorkoutDialog.StickyNote, is WorkoutDialog.Rest, is WorkoutDialog.Superset ->
            ExerciseDialogs(dialog, state, act, onDismiss)
    }
}

@Composable
private fun EndDialogs(
    dialog: WorkoutDialog,
    state: WorkoutUiState.Ready,
    onAction: (WorkoutAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val open = state.totalSets - state.completedSets
    when (dialog) {
        WorkoutDialog.Finish -> ConfirmDialog(
            title = stringResource(R.string.workout_finish_title),
            text = if (open == 0) "" else pluralStringResource(R.plurals.workout_finish_open_sets, open, open),
            confirmLabel = stringResource(R.string.workout_finish),
            dismissLabel = stringResource(R.string.workout_keep_logging),
            onConfirm = { onAction(WorkoutAction.Finish) },
            onDismiss = onDismiss,
        )

        WorkoutDialog.NothingLogged -> ConfirmDialog(
            title = stringResource(R.string.workout_finish_nothing_title),
            text = stringResource(R.string.workout_finish_nothing_text),
            confirmLabel = stringResource(UiR.string.ui_discard),
            dismissLabel = stringResource(R.string.workout_keep_logging),
            onConfirm = { onAction(WorkoutAction.Discard) },
            onDismiss = onDismiss,
            isDestructive = true,
        )

        else -> ConfirmDialog(
            title = stringResource(R.string.workout_discard_title),
            text = stringResource(R.string.workout_discard_text),
            confirmLabel = stringResource(UiR.string.ui_discard),
            dismissLabel = stringResource(R.string.workout_keep_logging),
            onConfirm = { onAction(WorkoutAction.Discard) },
            onDismiss = onDismiss,
            isDestructive = true,
        )
    }
}

@Composable
private fun ExerciseDialogs(
    dialog: WorkoutDialog,
    state: WorkoutUiState.Ready,
    onAction: (WorkoutAction) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is WorkoutDialog.ExerciseNotes -> TextInputDialog(
            title = stringResource(R.string.workout_exercise_note),
            initial = dialog.header.notes.orEmpty(),
            onConfirm = { onAction(WorkoutAction.SetNotes(dialog.header.id, it)) },
            onDismiss = onDismiss,
            singleLine = false,
        )

        is WorkoutDialog.StickyNote -> TextInputDialog(
            title = stringResource(R.string.workout_exercise_pinned_note),
            initial = dialog.header.stickyNote.orEmpty(),
            onConfirm = { onAction(WorkoutAction.SetStickyNote(dialog.header.exerciseId, it)) },
            onDismiss = onDismiss,
            singleLine = false,
        )

        is WorkoutDialog.Rest -> RestTimeDialog(
            selected = dialog.header.restSeconds,
            defaultSeconds = dialog.header.defaultRestSeconds,
            onSelect = { onAction(WorkoutAction.SetRest(dialog.header.id, it)) },
            onDismiss = onDismiss,
        )

        is WorkoutDialog.Superset -> SupersetDialog(
            others = state.exercises.filter { it.id != dialog.header.id },
            onConfirm = { onAction(WorkoutAction.Superset(dialog.header.id, it)) },
            onDismiss = onDismiss,
        )

        else -> Unit
    }
}

@Composable
private fun SupersetDialog(
    others: List<ExerciseRef>,
    onConfirm: (Set<WorkoutExerciseId>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<WorkoutExerciseId>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_superset_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (exercise in others) {
                    val isSelected = exercise.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = LiftLogDimens.minTouchTarget)
                            .toggleable(value = isSelected, role = Role.Checkbox) {
                                selected = if (isSelected) selected - exercise.id else selected + exercise.id
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null)
                        Text(exercise.name, Modifier.padding(start = LiftLogDimens.spacingM))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.workout_superset_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) } },
    )
}
