package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import kotlinx.coroutines.delay

private const val ADJUST_SECONDS = 15
private const val TRACK_ALPHA = 0.24f
private const val BUTTON_BORDER_ALPHA = 0.5f

/**
 * Prominent but non-blocking (SPEC §6): the time left, −15 s, +15 s and skip in one compact row, so that
 * the set being edited stays visible between the bar and the number pad.
 */
@Composable
fun RestTimerBar(
    state: RestTimerUiState,
    onAdjust: (seconds: Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    clock: () -> Long = System::currentTimeMillis,
) {
    val currentClock by rememberUpdatedState(clock)
    val remaining by produceState(initialValue = secondsLeft(state, currentClock()), state) {
        while (true) {
            val now = currentClock()
            value = secondsLeft(state, now)
            if (value == 0) break
            delay(millisUntilNextSecond(state, now))
        }
    }
    val fraction = if (state.totalSeconds > 0) remaining.toFloat() / state.totalSeconds else 0f
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LiftLogDimens.spacingM, vertical = LiftLogDimens.spacingS),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXs),
        ) {
            // The buttons wrap below the time on narrow screens and at large font sizes.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                TimeLeft(seconds = remaining)
                Row(horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
                    TimerButton(
                        text = stringResource(R.string.ui_timer_minus_15),
                        description = stringResource(R.string.ui_timer_subtract_description),
                        onClick = { onAdjust(-ADJUST_SECONDS) },
                    )
                    TimerButton(
                        text = stringResource(R.string.ui_timer_plus_15),
                        description = stringResource(R.string.ui_timer_add_description),
                        onClick = { onAdjust(ADJUST_SECONDS) },
                    )
                    TimerButton(text = stringResource(R.string.ui_timer_skip), description = null, onClick = onSkip)
                }
            }
            LinearWavyProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                trackColor = LocalContentColor.current.copy(alpha = TRACK_ALPHA),
            )
        }
    }
}

@Composable
private fun TimeLeft(seconds: Int, modifier: Modifier = Modifier) {
    val time = LiftLogFormat.duration(seconds)
    val over = stringResource(R.string.ui_timer_over)
    val description = if (seconds > 0) stringResource(R.string.ui_timer_remaining, time) else over
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(LiftLogIcons.Timer), contentDescription = null)
        Text(
            text = if (seconds > 0) time else over,
            modifier = Modifier
                .padding(start = LiftLogDimens.spacingS)
                .clearAndSetSemantics { contentDescription = description },
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
        )
    }
}

/** Outlined in the bar's own content color: the default outline and text are too faint on it. */
@Composable
private fun TimerButton(text: String, description: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .semantics { description?.let { contentDescription = it } },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.dp, color.copy(alpha = BUTTON_BORDER_ALPHA)),
        contentPadding = PaddingValues(horizontal = LiftLogDimens.spacingM),
    ) {
        Text(text = text, maxLines = 1)
    }
}
