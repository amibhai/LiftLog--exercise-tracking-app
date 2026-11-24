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
import io.github.amibhai.liftlog.core.ui.component.OptionsDialog
import io.github.amibhai.liftlog.core.ui.component.RestTimeDialog
import io.github.amibhai.liftlog.core.ui.component.SwitchRow
import io.github.amibhai.liftlog.core.ui.component.restTimeLabel
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.symbol
import java.math.BigDecimal
import io.github.amibhai.liftlog.core.ui.R as UiR

private val StepsKg = listOf(0.5, 1.0, 1.25, 2.0, 2.5, 5.0)
private val StepsLb = listOf(1.0, 2.5, 5.0, 10.0)
private val BarsKg = listOf(10.0, 15.0, 20.0, 25.0)
private val BarsLb = listOf(15.0, 25.0, 35.0, 45.0, 55.0)

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
internal fun SettingsDialogs(
    dialog: SettingsDialog,
    settings: UserSettings,
    onChange: (SettingChange) -> Unit,
    onDismiss: () -> Unit,
) {
    val unit = settings.weightUnit
    val choose: (SettingChange) -> Unit = {
        onDismiss()
        onChange(it)
    }
    when (dialog) {
        SettingsDialog.WEIGHT_STEP -> WeightDialog(
            title = stringResource(R.string.settings_weight_step),
            options = if (unit == WeightUnit.KG) StepsKg else StepsLb,
            selected = settings.weightStep,
            unit = unit,
            onSelect = { choose(SettingChange.WeightStep(it)) },
            onDismiss = onDismiss,
        )

        SettingsDialog.DUMBBELL_STEP -> WeightDialog(
            title = stringResource(R.string.settings_dumbbell_step),
            options = if (unit == WeightUnit.KG) StepsKg else StepsLb,
            selected = settings.dumbbellStep,
            unit = unit,
            onSelect = { choose(SettingChange.DumbbellStep(it)) },
            onDismiss = onDismiss,
        )

        SettingsDialog.BAR_WEIGHT -> WeightDialog(
            title = stringResource(R.string.settings_bar_weight),
            options = if (unit == WeightUnit.KG) BarsKg else BarsLb,
            selected = settings.barWeight,
            unit = unit,
            onSelect = { choose(SettingChange.BarWeight(it)) },
            onDismiss = onDismiss,
        )

        SettingsDialog.DEFAULT_REST -> RestTimeDialog(
            selected = settings.defaultRestSeconds,
            onSelect = { seconds -> seconds?.let { choose(SettingChange.DefaultRest(it)) } },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun WeightDialog(
    title: String,
    options: List<Double>,
    selected: Double,
    unit: WeightUnit,
    onSelect: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    OptionsDialog(
        title = title,
        options = (options + selected).distinct().sorted(),
        selected = selected,
        label = { weightText(it, unit) },
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(onClick = onClick, modifier = modifier, supportingContent = { Text(value) }) { Text(title) }
}

private fun weightText(value: Double, unit: WeightUnit) = LiftLogFormat.decimal(
    BigDecimal.valueOf(value),
) + " " + unit.symbol

@Composable
private fun effortName(mode: EffortMode): String = when (mode) {
    EffortMode.RPE -> stringResource(UiR.string.ui_rpe)
    EffortMode.RIR -> stringResource(UiR.string.ui_rir)
    EffortMode.OFF -> stringResource(R.string.settings_effort_off)
}

/** The dialogs of the weight and rest settings; an enum, so an open one survives rotation. */
internal enum class SettingsDialog { WEIGHT_STEP, DUMBBELL_STEP, BAR_WEIGHT, DEFAULT_REST }
