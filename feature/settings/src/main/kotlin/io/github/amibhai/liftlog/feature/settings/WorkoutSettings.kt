package io.github.amibhai.liftlog.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.SwitchRow
import io.github.amibhai.liftlog.core.ui.component.restTimeLabel
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.symbol
import java.math.BigDecimal
import io.github.amibhai.liftlog.core.ui.R as UiR

/** What −/+ add, and the bar weight, in the unit chosen above them. */
@Composable
internal fun WeightRows(settings: UserSettings, onOpen: (SettingsDialog) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        ValueRow(
            title = stringResource(R.string.settings_weight_step),
            value = weightText(settings.weightStep, settings.weightUnit),
            onClick = { onOpen(SettingsDialog.WEIGHT_STEP) },
        )
        ValueRow(
            title = stringResource(R.string.settings_dumbbell_step),
            value = weightText(settings.dumbbellStep, settings.weightUnit),
            onClick = { onOpen(SettingsDialog.DUMBBELL_STEP) },
        )
        ValueRow(
            title = stringResource(R.string.settings_bar_weight),
            value = weightText(settings.barWeight, settings.weightUnit),
            onClick = { onOpen(SettingsDialog.BAR_WEIGHT) },
        )
    }
}

/** Rest timer, effort and screen settings (SPEC §12). */
@Composable
internal fun WorkoutRows(
    settings: UserSettings,
    onChange: (SettingChange) -> Unit,
    onOpen: (SettingsDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ValueRow(
            title = stringResource(R.string.settings_default_rest),
            value = restTimeLabel(settings.defaultRestSeconds, defaultSeconds = null),
            onClick = { onOpen(SettingsDialog.DEFAULT_REST) },
        )
        SwitchRow(
            title = stringResource(R.string.settings_auto_rest),
            checked = settings.autoStartRestTimer,
            onCheckedChange = { onChange(SettingChange.AutoStartRest(it)) },
        )
        SwitchRow(
            title = stringResource(R.string.settings_timer_sound),
            checked = settings.restTimerSound,
            onCheckedChange = { onChange(SettingChange.TimerSound(it)) },
        )
        SwitchRow(
            title = stringResource(R.string.settings_timer_vibration),
            checked = settings.restTimerVibration,
            onCheckedChange = { onChange(SettingChange.TimerVibration(it)) },
        )
        ChoiceRow(
            title = stringResource(R.string.settings_effort),
            options = EffortMode.entries,
            selected = settings.effortMode,
            label = { effortName(it) },
            onSelect = { onChange(SettingChange.Effort(it)) },
        )
        SwitchRow(
            title = stringResource(R.string.settings_keep_screen_on),
            checked = settings.keepScreenOn,
            onCheckedChange = { onChange(SettingChange.KeepScreenOn(it)) },
        )
    }
}

@Composable
internal fun ValueRow(title: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(onClick = onClick, modifier = modifier, supportingContent = { Text(value) }) { Text(title) }
}

internal fun weightText(value: Double, unit: WeightUnit): String =
    LiftLogFormat.decimal(BigDecimal.valueOf(value)) + " " + unit.symbol

@Composable
private fun effortName(mode: EffortMode): String = when (mode) {
    EffortMode.RPE -> stringResource(UiR.string.ui_rpe)
    EffortMode.RIR -> stringResource(UiR.string.ui_rir)
    EffortMode.OFF -> stringResource(R.string.settings_effort_off)
}
