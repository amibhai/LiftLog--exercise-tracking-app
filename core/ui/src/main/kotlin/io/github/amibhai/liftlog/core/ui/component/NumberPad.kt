package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

private val MinStepFontSize = 12.sp

/** What the docked number pad shows for the field being edited. */
@Immutable
data class NumberPadUiState(
    /** e.g. "Weight (kg)". */
    val fieldLabel: String,
    /** The value as typed, already localized; empty when nothing is typed. */
    val valueText: String,
    /** The −/+ step in the display unit, e.g. "2.5"; null disables the step keys. */
    val stepLabel: String?,
    val allowsDecimal: Boolean,
    val canUsePrevious: Boolean,
    val isLastField: Boolean,
)

sealed interface NumberPadKey {
    data class Digit(val value: Int) : NumberPadKey

    data object Decimal : NumberPadKey

    data object Backspace : NumberPadKey

    data object StepDown : NumberPadKey

    data object StepUp : NumberPadKey

    data object SameAsPrevious : NumberPadKey

    data object Next : NumberPadKey

    data object Hide : NumberPadKey
}

/**
 * In-app number pad (SPEC §5): a non-modal bottom sheet docked under the list, so the row being edited
 * stays visible and other fields stay tappable. Big keys in the thumb zone; −/+ use the exercise step.
 */
@Composable
fun NumberPad(state: NumberPadUiState, onKey: (NumberPadKey) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val press: (NumberPadKey) -> Unit = { key ->
        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
        onKey(key)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = LiftLogDimens.spacingM, vertical = LiftLogDimens.spacingS),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            PadHeader(state = state, onHide = { press(NumberPadKey.Hide) })
            KeyRow {
                DigitKey(1, press)
                DigitKey(2, press)
                DigitKey(3, press)
                StepKey(up = false, stepLabel = state.stepLabel, onKey = press)
            }
            KeyRow {
                DigitKey(4, press)
                DigitKey(5, press)
                DigitKey(6, press)
                StepKey(up = true, stepLabel = state.stepLabel, onKey = press)
            }
            KeyRow {
                DigitKey(7, press)
                DigitKey(8, press)
                DigitKey(9, press)
                PadKey(
                    onClick = { press(NumberPadKey.SameAsPrevious) },
                    contentDescription = stringResource(R.string.ui_numpad_same_as_previous),
                    enabled = state.canUsePrevious,
                    tone = KeyTone.ACTION,
                ) {
                    Icon(painterResource(LiftLogIcons.Repeat), contentDescription = null)
                }
            }
            KeyRow {
                PadKey(
                    onClick = { press(NumberPadKey.Decimal) },
                    contentDescription = stringResource(R.string.ui_numpad_decimal),
                    enabled = state.allowsDecimal,
                ) {
                    Text(LiftLogFormat.decimalSeparator().toString(), style = MaterialTheme.typography.headlineSmall)
                }
                DigitKey(0, press)
                PadKey(
                    onClick = { press(NumberPadKey.Backspace) },
                    contentDescription = stringResource(R.string.ui_numpad_backspace),
                ) {
                    Icon(painterResource(LiftLogIcons.Backspace), contentDescription = null)
                }
                PadKey(
                    onClick = { press(NumberPadKey.Next) },
                    contentDescription = stringResource(
                        if (state.isLastField) R.string.ui_numpad_done else R.string.ui_numpad_next,
                    ),
                    tone = KeyTone.PRIMARY,
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.isLastField) LiftLogIcons.Check else LiftLogIcons.ArrowForward,
                        ),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun PadHeader(state: NumberPadUiState, onHide: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = state.fieldLabel,
            modifier = Modifier.weight(1f).padding(start = LiftLogDimens.spacingS),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = state.valueText.ifEmpty { stringResource(R.string.ui_set_previous_none) },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
        )
        IconButton(onClick = onHide) {
            Icon(
                painter = painterResource(LiftLogIcons.HideKeyboard),
                contentDescription = stringResource(R.string.ui_numpad_hide),
            )
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        content = content,
    )
}

@Composable
private fun RowScope.DigitKey(digit: Int, onKey: (NumberPadKey) -> Unit) {
    PadKey(onClick = { onKey(NumberPadKey.Digit(digit)) }, contentDescription = null) {
        Text(text = digit.toString(), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun RowScope.StepKey(up: Boolean, stepLabel: String?, onKey: (NumberPadKey) -> Unit) {
    val description = when {
        stepLabel == null -> null
        up -> stringResource(R.string.ui_numpad_step_up, stepLabel)
        else -> stringResource(R.string.ui_numpad_step_down, stepLabel)
    }
    PadKey(
        onClick = { onKey(if (up) NumberPadKey.StepUp else NumberPadKey.StepDown) },
        contentDescription = description,
        enabled = stepLabel != null,
        tone = KeyTone.ACTION,
    ) {
        val style = MaterialTheme.typography.titleLarge
        Text(
            text = (if (up) "+" else "−") + stepLabel.orEmpty(),
            // The key's own description ("Increase by 2.5") says it better.
            modifier = Modifier.clearAndSetSemantics {},
            autoSize = TextAutoSize.StepBased(minFontSize = MinStepFontSize, maxFontSize = style.fontSize),
            maxLines = 1,
            style = style,
        )
    }
}

private enum class KeyTone { DIGIT, ACTION, PRIMARY }

@Composable
private fun RowScope.PadKey(
    onClick: () -> Unit,
    contentDescription: String?,
    enabled: Boolean = true,
    tone: KeyTone = KeyTone.DIGIT,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = when (tone) {
        KeyTone.DIGIT -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )

        KeyTone.ACTION -> ButtonDefaults.filledTonalButtonColors()

        KeyTone.PRIMARY -> ButtonDefaults.buttonColors()
    }
    val label = contentDescription?.let { Modifier.semantics { this.contentDescription = it } } ?: Modifier
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(LiftLogDimens.numberPadKeyHeight)
            .then(label),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = colors,
        contentPadding = PaddingValues(horizontal = LiftLogDimens.spacingXs),
        content = content,
    )
}
