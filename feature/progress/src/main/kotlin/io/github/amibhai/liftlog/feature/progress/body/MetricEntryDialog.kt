package io.github.amibhai.liftlog.feature.progress.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.displayValue
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.DateTimeDialog
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.dateTimeText
import io.github.amibhai.liftlog.core.ui.format.unitText
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.progress.R
import io.github.amibhai.liftlog.core.ui.R as UiR

private const val MAX_VALUE = 2_000.0

/** One measurement: a new one, or [entry] to change. [onConfirm] gets the value as typed, in the unit in use. */
@Composable
internal fun MetricEntryDialog(
    type: BodyMetricType,
    unit: WeightUnit,
    entry: BodyMetric?,
    onConfirm: (shown: Double, at: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initial = entry?.let { LiftLogFormat.decimal(displayValue(type, it.value, unit)) }.orEmpty()
    val field = rememberTextFieldState(initial, TextRange(0, initial.length))
    val focus = remember { FocusRequester() }
    var at by rememberSaveable { mutableLongStateOf(entry?.measuredAt ?: System.currentTimeMillis()) }
    var isPickingTime by rememberSaveable { mutableStateOf(false) }
    val shown = parsePositive(field.text.toString())
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(type.labelRes())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM)) {
                OutlinedTextField(
                    state = field,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    suffix = { Text(type.unitText(unit)) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateTimeText(at),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { isPickingTime = true }) {
                        Text(stringResource(R.string.progress_metric_change_date))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { shown?.let { onConfirm(it, at) } }, enabled = shown != null) {
                Text(stringResource(UiR.string.ui_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) } },
    )
    if (isPickingTime) {
        DateTimeDialog(
            initial = at,
            onConfirm = {
                at = it
                isPickingTime = false
            },
            onDismiss = { isPickingTime = false },
        )
    }
}

/** "81,5" or "81.5"; null for anything that is no sensible measurement. */
private fun parsePositive(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it < MAX_VALUE }
