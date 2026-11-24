package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat

private val RestPresets = listOf(0, 30, 45, 60, 75, 90, 105, 120, 150, 180, 210, 240, 300)

/**
 * Picks a rest time with one tap. With a [defaultSeconds], null ("Default") is an option too; without one
 * the choice is always a number of seconds.
 */
@Composable
fun RestTimeDialog(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    defaultSeconds: Int? = null,
) {
    val presets = (RestPresets + listOfNotNull(selected)).distinct().sorted()
    OptionsDialog(
        title = stringResource(R.string.ui_rest_time),
        options = if (defaultSeconds == null) presets else listOf(null) + presets,
        selected = selected,
        label = { restTimeLabel(it, defaultSeconds) },
        onSelect = onSelect,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/** "Default (2:00)", "No rest timer", "1:30". */
@Composable
fun restTimeLabel(seconds: Int?, defaultSeconds: Int?): String = when (seconds) {
    null -> stringResource(R.string.ui_rest_default, restTimeLabel(defaultSeconds ?: 0, null))
    0 -> stringResource(R.string.ui_rest_off)
    else -> LiftLogFormat.duration(seconds)
}
