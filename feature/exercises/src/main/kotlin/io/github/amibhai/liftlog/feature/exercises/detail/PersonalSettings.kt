package io.github.amibhai.liftlog.feature.exercises.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.RestTimeDialog
import io.github.amibhai.liftlog.core.ui.component.TextInputDialog
import io.github.amibhai.liftlog.core.ui.component.restTimeLabel
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.format.symbol
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.feature.exercises.R
import java.math.BigDecimal
import io.github.amibhai.liftlog.core.ui.R as UiR

private enum class SettingDialog { REST, STEP, BAR, NOTE }

/** Rest time, weight step, bar weight and pinned note of one exercise (SPEC §2, §5). */
@Composable
internal fun PersonalSettings(
    exercise: ExerciseDetail,
    settings: UserSettings,
    actions: ExerciseDetailActions,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<SettingDialog?>(null) }
    Section(stringResource(R.string.exercises_detail_settings), modifier) {
        Column {
            SettingRow(
                title = stringResource(UiR.string.ui_rest_time),
                value = restTimeLabel(exercise.defaults.restSeconds, settings.defaultRestSeconds),
                onClick = { dialog = SettingDialog.REST },
            )
            if (SetField.WEIGHT in exercise.type.fields) {
                SettingRow(
                    title = stringResource(R.string.exercises_detail_step_size),
                    value = stepText(exercise, settings),
                    onClick = { dialog = SettingDialog.STEP },
                )
                SettingRow(
                    title = stringResource(R.string.exercises_detail_bar),
                    value = barText(exercise, settings),
                    onClick = { dialog = SettingDialog.BAR },
                )
            }
            SettingRow(
                title = stringResource(R.string.exercises_detail_note),
                value = exercise.notes ?: stringResource(R.string.exercises_detail_note_empty),
                onClick = { dialog = SettingDialog.NOTE },
            )
        }
    }
    dialog?.let { SettingDialogs(it, exercise, settings, actions, onDismiss = { dialog = null }) }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        supportingContent = { Text(value) },
        trailingContent = { Icon(painterResource(LiftLogIcons.Edit), contentDescription = null) },
    ) { Text(title) }
}

@Composable
private fun SettingDialogs(
    dialog: SettingDialog,
    exercise: ExerciseDetail,
    settings: UserSettings,
    actions: ExerciseDetailActions,
    onDismiss: () -> Unit,
) {
    val unit = settings.weightUnit
    val defaults = exercise.defaults
    when (dialog) {
        SettingDialog.REST -> RestTimeDialog(
            selected = defaults.restSeconds,
            defaultSeconds = settings.defaultRestSeconds,
            onSelect = {
                actions.onDefaultsChange(defaults.copy(restSeconds = it))
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        SettingDialog.STEP -> NumberDialog(
            title = stringResource(R.string.exercises_detail_step_size),
            unit = unit,
            initial = exercise.ownStep(unit)?.let(BigDecimal::valueOf),
            onConfirm = { value -> actions.onDefaultsChange(defaults.withStep(value?.toDouble(), unit)) },
            onDismiss = onDismiss,
        )

        SettingDialog.BAR -> NumberDialog(
            title = stringResource(R.string.exercises_detail_bar),
            unit = unit,
            initial = defaults.barWeight?.displayValue(unit),
            onConfirm = { value ->
                actions.onDefaultsChange(defaults.copy(barWeight = value?.let { Weight.of(it, unit) }))
            },
            onDismiss = onDismiss,
        )

        SettingDialog.NOTE -> TextInputDialog(
            title = stringResource(R.string.exercises_detail_note),
            initial = exercise.notes.orEmpty(),
            onConfirm = {
                actions.onNoteChange(it)
                onDismiss()
            },
            onDismiss = onDismiss,
            singleLine = false,
        )
    }
}

/** A weight in the active unit; an empty or unreadable value means the default. */
@Composable
private fun NumberDialog(
    title: String,
    unit: WeightUnit,
    initial: BigDecimal?,
    onConfirm: (BigDecimal?) -> Unit,
    onDismiss: () -> Unit,
) {
    TextInputDialog(
        title = title,
        initial = initial?.let { LiftLogFormat.decimal(it) }.orEmpty(),
        label = stringResource(R.string.exercises_detail_value_label, unit.symbol),
        keyboardType = KeyboardType.Decimal,
        onConfirm = { text ->
            val value = text.replace(LiftLogFormat.decimalSeparator(), '.').toBigDecimalOrNull()
            onConfirm(value?.takeIf { it.signum() > 0 && it < MaxWeight })
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun stepText(exercise: ExerciseDetail, settings: UserSettings): String {
    val unit = settings.weightUnit
    val own = exercise.ownStep(unit)
    val step = own ?: settings.weightStep(exercise.equipment, unit)
    val text = LiftLogFormat.decimal(BigDecimal.valueOf(step)) + " " + unit.symbol
    return if (own == null) stringResource(R.string.exercises_detail_default_value, text) else text
}

@Composable
private fun barText(exercise: ExerciseDetail, settings: UserSettings): String {
    val text = SetText(settings.weightUnit)
    val own = exercise.defaults.barWeight
    val bar = own ?: settings.barWeight(exercise.equipment, settings.weightUnit) ?: Weight.ZERO
    val shown = text.weight(bar)
    return if (own == null) stringResource(R.string.exercises_detail_default_value, shown) else shown
}

private fun ExerciseDefaults.withStep(step: Double?, unit: WeightUnit) = when (unit) {
    WeightUnit.KG -> copy(weightStepKg = step)
    WeightUnit.LB -> copy(weightStepLb = step)
}

private fun ExerciseDetail.ownStep(unit: WeightUnit): Double? =
    if (unit == WeightUnit.KG) defaults.weightStepKg else defaults.weightStepLb

private val MaxWeight = BigDecimal(1_000)
