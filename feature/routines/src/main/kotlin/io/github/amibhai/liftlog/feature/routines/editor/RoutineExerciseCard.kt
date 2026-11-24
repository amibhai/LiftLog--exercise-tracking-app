package io.github.amibhai.liftlog.feature.routines.editor

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.effortText
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.routines.R
import io.github.amibhai.liftlog.core.ui.R as UiR

/** Most sets a routine plans for one exercise. */
internal const val MAX_PLANNED_SETS = 20

@Composable
internal fun RoutineExerciseCard(
    exercise: RoutineExerciseUiModel,
    effortMode: EffortMode,
    onAction: (RoutineEditorAction) -> Unit,
    onOpenDialog: (RoutineDialog) -> Unit,
    onOpenExercise: () -> Unit,
    onReplace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingS) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            exercise.supersetLetter?.let {
                Text(
                    text = stringResource(R.string.routine_superset, it.toString()),
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
                    .clickable(role = Role.Button, onClick = onOpenExercise)
                    .padding(vertical = LiftLogDimens.spacingM)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ExerciseMenu(exercise, onAction, onOpenDialog, onReplace)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            SetsStepper(exercise.targetSets, onChange = { onAction(RoutineEditorAction.SetSets(exercise.id, it)) })
            AssistChip(
                onClick = { onOpenDialog(RoutineDialog.Reps(exercise)) },
                label = { Text(repsLabel(exercise.repMin, exercise.repMax)) },
            )
            AssistChip(
                onClick = { onOpenDialog(RoutineDialog.Rest(exercise)) },
                label = { Text(restLabel(exercise.restSeconds ?: exercise.defaultRestSeconds)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(LiftLogIcons.Timer),
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
            if (effortMode != EffortMode.OFF) {
                AssistChip(
                    onClick = { onOpenDialog(RoutineDialog.Effort(exercise)) },
                    label = { Text(effortLabel(effortMode, exercise.effortTarget)) },
                )
            }
        }
        exercise.notes?.let {
            Text(
                text = it,
                modifier = Modifier
                    .clickable(onClick = { onOpenDialog(RoutineDialog.Notes(exercise)) })
                    .padding(vertical = LiftLogDimens.spacingXs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetsStepper(sets: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(sets - 1) }, enabled = sets > 1) {
            Icon(painterResource(LiftLogIcons.Remove), stringResource(R.string.routine_fewer_sets))
        }
        Text(pluralStringResource(R.plurals.routine_sets, sets, sets), style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onChange(sets + 1) }, enabled = sets < MAX_PLANNED_SETS) {
            Icon(painterResource(LiftLogIcons.Add), stringResource(R.string.routine_more_sets))
        }
    }
}

@Composable
private fun ExerciseMenu(
    exercise: RoutineExerciseUiModel,
    onAction: (RoutineEditorAction) -> Unit,
    onOpenDialog: (RoutineDialog) -> Unit,
    onReplace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    val choose: (() -> Unit) -> Unit = { action ->
        isOpen = false
        action()
    }
    Box(modifier) {
        IconButton(onClick = { isOpen = true }) {
            Icon(painterResource(LiftLogIcons.More), stringResource(R.string.routine_exercise_options, exercise.name))
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            MenuItem(R.string.routine_exercise_note, LiftLogIcons.Note) {
                choose { onOpenDialog(RoutineDialog.Notes(exercise)) }
            }
            MenuItem(R.string.routine_exercise_replace, LiftLogIcons.Replace) { choose(onReplace) }
            if (exercise.supersetLetter == null) {
                MenuItem(R.string.routine_exercise_superset, LiftLogIcons.Link) {
                    choose { onOpenDialog(RoutineDialog.Superset(exercise)) }
                }
            } else {
                MenuItem(R.string.routine_exercise_leave_superset, LiftLogIcons.LinkOff) {
                    choose { onAction(RoutineEditorAction.LeaveSuperset(exercise.id)) }
                }
            }
            MenuItem(R.string.routine_exercise_remove, LiftLogIcons.Delete) {
                choose { onAction(RoutineEditorAction.Remove(exercise.id)) }
            }
        }
    }
}

@Composable
private fun MenuItem(@StringRes label: Int, @DrawableRes icon: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = onClick,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
    )
}

/** "8 reps", "8–12 reps", "Any reps". */
@Composable
internal fun repsLabel(min: Int?, max: Int?): String {
    val low = min ?: max
    val high = max ?: min
    return when {
        low == null || high == null -> stringResource(R.string.routine_reps_any)
        low == high -> pluralStringResource(R.plurals.routine_reps, high, "$high")
        else -> pluralStringResource(R.plurals.routine_reps, high, "$low–$high")
    }
}

/** "Rest 2:00", or "No rest" when the timer is off. */
@Composable
private fun restLabel(seconds: Int): String = if (seconds == 0) {
    stringResource(R.string.routine_no_rest)
} else {
    stringResource(R.string.routine_rest, LiftLogFormat.duration(seconds))
}

/** "RPE 8", or "No RPE target". */
@Composable
internal fun effortLabel(mode: EffortMode, value: Double?): String {
    val name = stringResource(if (mode == EffortMode.RIR) UiR.string.ui_rir else UiR.string.ui_rpe)
    return value?.let { stringResource(UiR.string.ui_effort_value, name, effortText(it)) }
        ?: stringResource(R.string.routine_effort_none, name)
}
