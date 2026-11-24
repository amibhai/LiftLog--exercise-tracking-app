package io.github.amibhai.liftlog.feature.workout.logging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.ui.component.ConfirmDialog
import io.github.amibhai.liftlog.core.ui.component.DateTimeDialog
import io.github.amibhai.liftlog.core.ui.component.MultiChoiceDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderItem
import io.github.amibhai.liftlog.core.ui.component.RestTimeDialog
import io.github.amibhai.liftlog.core.ui.component.TextInputDialog
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

        is WorkoutDialog.Superset -> MultiChoiceDialog(
            title = stringResource(R.string.workout_superset_title),
            options = state.exercises.filter { it.id != dialog.header.id },
            label = { it.name },
            confirmLabel = stringResource(R.string.workout_superset_confirm),
            onConfirm = { picked ->
                onAction(
                    WorkoutAction.Superset(dialog.header.id, picked.mapTo(mutableSetOf()) { it.id }),
                )
            },
            onDismiss = onDismiss,
        )

        else -> Unit
    }
}
