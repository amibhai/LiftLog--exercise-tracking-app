package io.github.amibhai.liftlog.feature.workout.logging

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.amibhai.liftlog.core.ui.component.NumberPad
import io.github.amibhai.liftlog.core.ui.component.NumberPadUiState
import io.github.amibhai.liftlog.core.ui.component.PlateBar
import io.github.amibhai.liftlog.core.ui.component.RestTimerBar
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.workoutTitle
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.workout.R
import kotlinx.coroutines.delay
import io.github.amibhai.liftlog.core.ui.R as UiR

private const val MILLIS_PER_SECOND = 1_000L

/** Title, finish and menu, above time, volume and sets done. */
@Composable
internal fun WorkoutTopBar(
    state: WorkoutUiState.Ready,
    onClose: () -> Unit,
    onOpenDialog: (WorkoutDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TopAppBar(
            title = { Text(workoutTitle(state.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    if (state.isInProgress) {
                        Icon(painterResource(LiftLogIcons.ExpandMore), stringResource(R.string.workout_minimize))
                    } else {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                }
            },
            actions = {
                if (state.isInProgress) {
                    FilledTonalButton(onClick = { onOpenDialog(finishDialog(state)) }) {
                        Text(stringResource(R.string.workout_finish))
                    }
                }
                WorkoutMenu(state, onOpenDialog)
            },
        )
        SummaryRow(state)
    }
}

@Composable
private fun WorkoutMenu(
    state: WorkoutUiState.Ready,
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
            Icon(painterResource(LiftLogIcons.More), stringResource(UiR.string.ui_more_options))
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            MenuItem(stringResource(R.string.workout_rename), LiftLogIcons.Edit) { open(WorkoutDialog.Rename) }
            MenuItem(stringResource(R.string.workout_notes), LiftLogIcons.Note) { open(WorkoutDialog.Notes) }
            if (state.exercises.size > 1) {
                MenuItem(stringResource(R.string.workout_reorder), LiftLogIcons.Reorder) { open(WorkoutDialog.Reorder) }
            }
            if (state.isInProgress) {
                MenuItem(stringResource(R.string.workout_discard), LiftLogIcons.Delete) { open(WorkoutDialog.Discard) }
            }
        }
    }
}

@Composable
internal fun MenuItem(label: String, @DrawableRes icon: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
    )
}

@Composable
private fun SummaryRow(state: WorkoutUiState.Ready, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingS)) {
            Summary(stringResource(R.string.workout_duration_label)) { ElapsedTime(state.startedAt, state.endedAt) }
            Summary(stringResource(R.string.workout_volume_label)) { SummaryValue(state.volume) }
            Summary(stringResource(R.string.workout_sets_label)) {
                SummaryValue(stringResource(R.string.workout_sets_value, state.completedSets, state.totalSets))
            }
        }
    }
}

@Composable
private fun RowScope.Summary(label: String, value: @Composable () -> Unit) {
    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        value()
    }
}

@Composable
private fun SummaryValue(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = MaterialTheme.typography.titleMedium)
}

/** Ticks on its own, so that only this text recomposes every second. */
@Composable
internal fun ElapsedTime(startedAt: Long, endedAt: Long?, modifier: Modifier = Modifier) {
    val now by produceState(System.currentTimeMillis(), startedAt, endedAt) {
        while (endedAt == null) {
            value = System.currentTimeMillis()
            delay(MILLIS_PER_SECOND - (value - startedAt).mod(MILLIS_PER_SECOND))
        }
    }
    val seconds = ((endedAt ?: now) - startedAt).coerceAtLeast(0) / MILLIS_PER_SECOND
    SummaryValue(LiftLogFormat.duration(seconds.toInt()), modifier)
}

/** The rest timer and the number pad, docked below the list. */
@Composable
internal fun BottomControls(
    state: WorkoutUiState.Ready,
    onAction: (WorkoutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timer = state.restTimer
    val pad = state.pad
    if (timer == null && pad == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // The number pad pads itself above the navigation bar; the timer alone needs it here.
            .then(if (pad == null) Modifier.navigationBarsPadding() else Modifier),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (timer != null) {
            RestTimerBar(
                state = timer,
                onAdjust = { onAction(WorkoutAction.AdjustRest(it)) },
                onSkip = { onAction(WorkoutAction.SkipRest) },
                modifier = Modifier.padding(LiftLogDimens.spacingS),
            )
        }
        if (pad != null) {
            NumberPad(
                state = NumberPadUiState(
                    fieldLabel = fieldLabel(pad),
                    valueText = pad.display,
                    stepLabel = pad.step,
                    allowsDecimal = pad.allowsDecimal,
                    canUsePrevious = pad.canUsePrevious,
                    isLastField = pad.isLastField,
                ),
                onKey = { onAction(WorkoutAction.PadKey(it)) },
                hint = pad.plates?.let { plates -> { PlateBar(plates) } },
            )
        }
    }
}

@Composable
private fun fieldLabel(pad: PadUiModel): String {
    val name = stringResource(pad.field.labelRes())
    return pad.unit?.let { "$name ($it)" } ?: name
}
