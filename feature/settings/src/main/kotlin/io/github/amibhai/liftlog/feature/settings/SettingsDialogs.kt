package io.github.amibhai.liftlog.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.OptionsDialog
import io.github.amibhai.liftlog.core.ui.component.RestTimeDialog

private val StepsKg = listOf(0.5, 1.0, 1.25, 2.0, 2.5, 5.0)
private val StepsLb = listOf(1.0, 2.5, 5.0, 10.0)
private val BarsKg = listOf(10.0, 15.0, 20.0, 25.0)
private val BarsLb = listOf(15.0, 25.0, 35.0, 45.0, 55.0)

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

        SettingsDialog.WEEKLY_SETS -> WeeklySetsDialog(
            target = settings.weeklySetTarget,
            onSave = { least, most -> choose(SettingChange.WeeklySets(least, most)) },
            onDismiss = onDismiss,
        )

        SettingsDialog.FIRST_DAY -> FirstDayDialog(
            selected = settings.firstDayOfWeek,
            onSelect = { choose(SettingChange.FirstDay(it)) },
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

/** An enum, so an open dialog survives rotation. */
internal enum class SettingsDialog { WEIGHT_STEP, DUMBBELL_STEP, BAR_WEIGHT, DEFAULT_REST, WEEKLY_SETS, FIRST_DAY }
