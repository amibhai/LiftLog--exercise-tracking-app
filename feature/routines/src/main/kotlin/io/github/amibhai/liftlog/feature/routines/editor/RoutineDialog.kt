package io.github.amibhai.liftlog.feature.routines.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.workout.RepRange
import io.github.amibhai.liftlog.core.domain.workout.scale
import io.github.amibhai.liftlog.core.ui.component.MultiChoiceDialog
import io.github.amibhai.liftlog.core.ui.component.OptionsDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderDialog
import io.github.amibhai.liftlog.core.ui.component.ReorderItem
import io.github.amibhai.liftlog.core.ui.component.RestTimeDialog
import io.github.amibhai.liftlog.core.ui.component.TextInputDialog
import io.github.amibhai.liftlog.feature.routines.R

/** The dialog that is open, if any. */
internal sealed interface RoutineDialog {
    data object Rename : RoutineDialog

    data object Reorder : RoutineDialog

    data class Reps(val exercise: RoutineExerciseUiModel) : RoutineDialog

    data class Rest(val exercise: RoutineExerciseUiModel) : RoutineDialog

    data class Effort(val exercise: RoutineExerciseUiModel) : RoutineDialog

    data class Progression(val exercise: RoutineExerciseUiModel) : RoutineDialog

    data class Notes(val exercise: RoutineExerciseUiModel) : RoutineDialog

    data class Superset(val exercise: RoutineExerciseUiModel) : RoutineDialog
}

/** Common targets, single numbers before ranges; one tap picks one. */
private val RepPresets: List<RepRange> =
    listOf(1, 3, 5, 6, 8, 10, 12, 15, 20).map { RepRange(it, it) } +
        listOf(3 to 5, 4 to 6, 5 to 8, 6 to 8, 6 to 10, 8 to 10, 8 to 12, 10 to 12, 10 to 15, 12 to 15)
            .plus(listOf(15 to 20, 20 to 30))
            .map { (min, max) -> RepRange(min, max) }

@Composable
internal fun RoutineDialogs(
    dialog: RoutineDialog,
    state: RoutineEditorUiState.Ready,
    onAction: (RoutineEditorAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val act: (RoutineEditorAction) -> Unit = {
        onDismiss()
        onAction(it)
    }
    when (dialog) {
        RoutineDialog.Rename -> TextInputDialog(
            title = stringResource(R.string.routine_name),
            initial = state.name,
            onConfirm = { act(RoutineEditorAction.Rename(it)) },
            onDismiss = onDismiss,
        )

        RoutineDialog.Reorder -> ReorderDialog(
            title = stringResource(R.string.routine_reorder),
            items = state.exercises.map { ReorderItem(it.id.value, it.name, it.supersetLetter?.toString()) },
            onConfirm = { ids -> act(RoutineEditorAction.Reorder(ids.map(::RoutineExerciseId))) },
            onDismiss = onDismiss,
        )

        is RoutineDialog.Reps -> RepsDialog(dialog.exercise, act, onDismiss)

        is RoutineDialog.Rest -> RestTimeDialog(
            selected = dialog.exercise.restSeconds,
            onSelect = { act(RoutineEditorAction.SetRest(dialog.exercise.id, it)) },
            onDismiss = onDismiss,
            defaultSeconds = dialog.exercise.defaultRestSeconds,
        )

        is RoutineDialog.Effort -> OptionsDialog(
            title = stringResource(R.string.routine_effort_title),
            options = listOf(null) + state.effortMode.scale,
            selected = dialog.exercise.effortTarget,
            label = { effortLabel(state.effortMode, it) },
            onSelect = { act(RoutineEditorAction.SetEffort(dialog.exercise.id, it)) },
            onDismiss = onDismiss,
        )

        is RoutineDialog.Progression -> ProgressionDialog(
            current = dialog.exercise.progression,
            unit = state.unit,
            onSave = { act(RoutineEditorAction.SetProgression(dialog.exercise.id, it)) },
            onDismiss = onDismiss,
        )

        is RoutineDialog.Notes -> TextInputDialog(
            title = stringResource(R.string.routine_exercise_note),
            initial = dialog.exercise.notes.orEmpty(),
            onConfirm = { act(RoutineEditorAction.SetNotes(dialog.exercise.id, it)) },
            onDismiss = onDismiss,
            singleLine = false,
        )

        is RoutineDialog.Superset -> MultiChoiceDialog(
            title = stringResource(R.string.routine_superset_title),
            options = state.exercises.filter { it.id != dialog.exercise.id },
            label = { it.name },
            confirmLabel = stringResource(R.string.routine_superset_confirm),
            onConfirm = { picked ->
                act(RoutineEditorAction.Superset(dialog.exercise.id, picked.mapTo(mutableSetOf()) { it.id }))
            },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun RepsDialog(
    exercise: RoutineExerciseUiModel,
    onAction: (RoutineEditorAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val min = exercise.repMin ?: exercise.repMax
    val max = exercise.repMax ?: exercise.repMin
    val current = if (min != null && max != null) RepRange(min, max) else null
    val ranges = (RepPresets + listOfNotNull(current)).distinct()
        .sortedWith(compareBy<RepRange>({ it.min != it.max }, { it.min }, { it.max }))
    OptionsDialog(
        title = stringResource(R.string.routine_reps_title),
        options = listOf(null) + ranges,
        selected = current,
        label = { repsLabel(it?.min, it?.max) },
        onSelect = { onAction(RoutineEditorAction.SetReps(exercise.id, it?.min, it?.max)) },
        onDismiss = onDismiss,
    )
}
