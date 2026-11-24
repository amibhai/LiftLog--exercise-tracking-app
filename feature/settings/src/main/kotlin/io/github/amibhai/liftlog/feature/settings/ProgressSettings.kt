package io.github.amibhai.liftlog.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.progress.E1rmFormula
import io.github.amibhai.liftlog.core.ui.component.NumberStepper
import io.github.amibhai.liftlog.core.ui.component.OptionsDialog
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import io.github.amibhai.liftlog.core.ui.R as UiR

/** Null follows the phone's language. */
private val FirstDays = listOf(null, DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)

/** What part of a set counts for a secondary muscle. */
private val SecondaryShares = listOf(0.0, 0.5, 1.0)

/** Most weekly sets a target can ask for. */
private const val MAX_WEEKLY_SETS = 40

/** How progress is worked out (SPEC §8, §12). */
@Composable
internal fun ProgressRows(
    settings: UserSettings,
    onChange: (SettingChange) -> Unit,
    onOpen: (SettingsDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ValueRow(
            title = stringResource(R.string.settings_weekly_sets),
            value = "${settings.weeklySetTargetMin}–${settings.weeklySetTargetMax}",
            onClick = { onOpen(SettingsDialog.WEEKLY_SETS) },
        )
        ChoiceRow(
            title = stringResource(R.string.settings_secondary_muscles),
            options = SecondaryShares,
            selected = settings.secondaryMuscleSets,
            label = { stringResource(secondaryLabelRes(it)) },
            onSelect = { onChange(SettingChange.SecondaryMuscles(it)) },
        )
        ChoiceRow(
            title = stringResource(R.string.settings_formula),
            options = E1rmFormula.entries,
            selected = settings.e1rmFormula,
            label = { stringResource(it.labelRes()) },
            onSelect = { onChange(SettingChange.Formula(it)) },
        )
        ValueRow(
            title = stringResource(R.string.settings_first_day),
            value = firstDayLabel(settings.firstDayOfWeek),
            onClick = { onOpen(SettingsDialog.FIRST_DAY) },
        )
    }
}

@Composable
internal fun FirstDayDialog(selected: DayOfWeek?, onSelect: (DayOfWeek?) -> Unit, onDismiss: () -> Unit) {
    OptionsDialog(
        title = stringResource(R.string.settings_first_day),
        options = FirstDays,
        selected = selected,
        label = { firstDayLabel(it) },
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

/** The sets per muscle and week to aim for. */
@Composable
internal fun WeeklySetsDialog(target: IntRange, onSave: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var least by rememberSaveable { mutableIntStateOf(target.first) }
    var most by rememberSaveable { mutableIntStateOf(target.last) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_weekly_sets)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
                StepperRow(stringResource(R.string.settings_weekly_sets_min), least, 1..most, onChange = { least = it })
                StepperRow(
                    label = stringResource(R.string.settings_weekly_sets_max),
                    value = most,
                    range = least..MAX_WEEKLY_SETS,
                    onChange = { most = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(least, most) }) { Text(stringResource(UiR.string.ui_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) } },
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        NumberStepper(
            value = value,
            range = range,
            onChange = onChange,
            fewer = stringResource(R.string.settings_fewer, label),
            more = stringResource(R.string.settings_more, label),
        )
    }
}

@Composable
private fun firstDayLabel(day: DayOfWeek?): String =
    day?.let(::dayName) ?: stringResource(R.string.settings_first_day_auto, dayName(UserSettings().weekStartsOn()))

private fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())

private fun secondaryLabelRes(share: Double): Int = when (share) {
    0.0 -> R.string.settings_secondary_none
    1.0 -> R.string.settings_secondary_full
    else -> R.string.settings_secondary_half
}

private fun E1rmFormula.labelRes(): Int = when (this) {
    E1rmFormula.EPLEY -> R.string.settings_formula_epley
    E1rmFormula.BRZYCKI -> R.string.settings_formula_brzycki
}
