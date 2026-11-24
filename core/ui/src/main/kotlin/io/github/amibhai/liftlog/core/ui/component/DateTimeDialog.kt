package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Picks a date, then a time. [initial] and the result are epoch milliseconds; [zone] is the phone's. */
@Composable
fun DateTimeDialog(
    initial: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val start = remember(initial, zone) { Instant.ofEpochMilli(initial).atZone(zone) }
    var date by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val picked = date
    val cancel = @Composable { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } }
    if (picked == null) {
        val state = rememberDatePickerState(initialSelectedDate = start.toLocalDate())
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { date = state.getSelectedDate() ?: start.toLocalDate() }) {
                    Text(stringResource(R.string.ui_next))
                }
            },
            modifier = modifier,
            dismissButton = cancel,
        ) { DatePicker(state = state) }
    } else {
        val state = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute)
        TimePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(picked.atTime(state.hour, state.minute).atZone(zone).toInstant().toEpochMilli())
                    },
                ) { Text(stringResource(R.string.ui_done)) }
            },
            title = { Text(stringResource(R.string.ui_select_time)) },
            modifier = modifier,
            dismissButton = cancel,
        ) { TimePicker(state = state) }
    }
}
