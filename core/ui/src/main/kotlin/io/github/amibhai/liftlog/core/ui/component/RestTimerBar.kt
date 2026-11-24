package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
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
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import kotlinx.coroutines.delay

private const val ADJUST_SECONDS = 15

/** Prominent but non-blocking: −15 s, +15 s and skip, with a wavy progress bar. */
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
    val time = LiftLogFormat.duration(remaining)
    val description = if (remaining > 0) {
        stringResource(R.string.ui_timer_remaining, time)
    } else {
        stringResource(R.string.ui_timer_over)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(LiftLogDimens.spacingM),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(LiftLogIcons.Timer), contentDescription = null)
                Text(
                    text = if (remaining > 0) time else stringResource(R.string.ui_timer_over),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = LiftLogDimens.spacingS)
                        .clearAndSetSemantics { contentDescription = description },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                )
            }
            val fraction = if (state.totalSeconds > 0) remaining.toFloat() / state.totalSeconds else 0f
            LinearWavyProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
                val subtract = stringResource(R.string.ui_timer_subtract_description)
                val add = stringResource(R.string.ui_timer_add_description)
                OutlinedButton(
                    onClick = { onAdjust(-ADJUST_SECONDS) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = LiftLogDimens.minTouchTarget)
                        .semantics { contentDescription = subtract },
                ) { Text(stringResource(R.string.ui_timer_minus_15)) }
                OutlinedButton(
                    onClick = { onAdjust(ADJUST_SECONDS) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = LiftLogDimens.minTouchTarget)
                        .semantics { contentDescription = add },
                ) { Text(stringResource(R.string.ui_timer_plus_15)) }
                FilledTonalButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = LiftLogDimens.minTouchTarget),
                ) {
                    Icon(painterResource(LiftLogIcons.SkipNext), contentDescription = null)
                    Text(stringResource(R.string.ui_timer_skip))
                }
            }
        }
    }
}
