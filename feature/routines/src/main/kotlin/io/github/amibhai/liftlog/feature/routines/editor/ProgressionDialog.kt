package io.github.amibhai.liftlog.feature.routines.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.ProgressionRuleType
import io.github.amibhai.liftlog.core.domain.model.Stage
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.NumberStepper
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.format.symbol
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.routines.R
import java.math.BigDecimal
import io.github.amibhai.liftlog.core.ui.R as UiR

private const val MOST_MISSES = 6
private val MaxWeight = BigDecimal(2_000)
private val DeloadPercents = listOf(5, 10, 15, 20)
private val IncrementsKg = listOf(1.25, 2.5, 5.0)
private val IncrementsLb = listOf(2.5, 5.0, 10.0)

/** Sets and reps that follow each other; the ones GZCLP uses. */
private val StagePresets = listOf(
    listOf(Stage(sets = 5, reps = 3), Stage(sets = 6, reps = 2), Stage(sets = 10, reps = 1)),
    listOf(Stage(sets = 3, reps = 10), Stage(sets = 3, reps = 8), Stage(sets = 3, reps = 6)),
)

/** How the exercise moves on from session to session (SPEC §7). */
@Composable
internal fun ProgressionDialog(
    current: ProgressionRule,
    unit: WeightUnit,
    onSave: (ProgressionRule) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Rules keep their settings while another one is tried.
    var drafts by remember { mutableStateOf(defaultRules() + (current.type to current)) }
    var selected by remember { mutableStateOf(current.type) }
    val rule = drafts.getValue(selected)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.routine_progression)) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                for (type in ProgressionRuleType.entries) {
                    RuleOption(type, isSelected = type == selected, onSelect = { selected = type })
                    if (type == selected) {
                        RuleSettings(rule, unit, onChange = { drafts = drafts + (it.type to it) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(rule) }) { Text(stringResource(UiR.string.ui_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) } },
    )
}

private fun defaultRules(): Map<ProgressionRuleType, ProgressionRule> = listOf(
    ProgressionRule.DoubleProgression,
    ProgressionRule.Linear(),
    ProgressionRule.Stages(StagePresets.first()),
    ProgressionRule.FiveThreeOne(),
    ProgressionRule.None,
).associateBy { it.type }

@Composable
private fun RuleOption(
    type: ProgressionRuleType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = LiftLogDimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Column {
            Text(stringResource(type.nameRes()), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(type.descriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RuleSettings(
    rule: ProgressionRule,
    unit: WeightUnit,
    onChange: (ProgressionRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = LiftLogDimens.spacingXxl, bottom = LiftLogDimens.spacingS),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXs),
    ) {
        when (rule) {
            is ProgressionRule.Linear -> LinearSettings(rule, unit, onChange)
            is ProgressionRule.Stages -> StagesSettings(rule, unit, onChange)
            is ProgressionRule.FiveThreeOne -> FiveThreeOneSettings(rule, unit, onChange)
            ProgressionRule.DoubleProgression, ProgressionRule.None -> Unit
        }
    }
}

@Composable
private fun ColumnScope.LinearSettings(
    rule: ProgressionRule.Linear,
    unit: WeightUnit,
    onChange: (ProgressionRule) -> Unit,
) {
    IncrementChips(
        label = stringResource(R.string.routine_rule_increment),
        selected = rule.incrementKg,
        unit = unit,
        hasDefault = true,
        onSelect = { onChange(rule.copy(incrementKg = it)) },
    )
    SettingLabel(stringResource(R.string.routine_rule_misses))
    NumberStepper(
        value = rule.missesBeforeDeload,
        range = 1..MOST_MISSES,
        onChange = { onChange(rule.copy(missesBeforeDeload = it)) },
        fewer = stringResource(R.string.routine_rule_fewer_misses),
        more = stringResource(R.string.routine_rule_more_misses),
    )
    PercentChips(
        label = stringResource(R.string.routine_rule_deload),
        selected = rule.deloadPercent,
        onSelect = { onChange(rule.copy(deloadPercent = it)) },
    )
}

@Composable
private fun ColumnScope.StagesSettings(
    rule: ProgressionRule.Stages,
    unit: WeightUnit,
    onChange: (ProgressionRule) -> Unit,
) {
    SettingLabel(stringResource(R.string.routine_rule_stages_label))
    Chips((StagePresets + listOf(rule.stages)).distinct(), rule.stages, { stagesText(it) }) {
        onChange(rule.copy(stages = it))
    }
    IncrementChips(
        label = stringResource(R.string.routine_rule_increment),
        selected = rule.incrementKg,
        unit = unit,
        hasDefault = true,
        onSelect = { onChange(rule.copy(incrementKg = it)) },
    )
    PercentChips(
        label = stringResource(R.string.routine_rule_reset),
        selected = rule.resetPercent,
        onSelect = { onChange(rule.copy(resetPercent = it)) },
    )
}

@Composable
private fun ColumnScope.FiveThreeOneSettings(
    rule: ProgressionRule.FiveThreeOne,
    unit: WeightUnit,
    onChange: (ProgressionRule) -> Unit,
) {
    val initial = rule.trainingMaxKg?.let { LiftLogFormat.weight(Weight(it), unit) }.orEmpty()
    val field = rememberTextFieldState(initial)
    val current by rememberUpdatedState(rule)
    val change by rememberUpdatedState(onChange)
    LaunchedEffect(field) {
        snapshotFlow { field.text.toString() }.collect { text ->
            val value = text.replace(LiftLogFormat.decimalSeparator(), '.').toBigDecimalOrNull()
                ?.takeIf { it.signum() > 0 && it < MaxWeight }
            // Only what was typed changes the stored weight, so that it never drifts.
            if (text != initial) change(current.copy(trainingMaxKg = value?.let { Weight.of(it, unit).kg }))
        }
    }
    OutlinedTextField(
        state = field,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.routine_rule_training_max, unit.symbol)) },
        supportingText = { Text(stringResource(R.string.routine_rule_training_max_hint)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    IncrementChips(
        label = stringResource(R.string.routine_rule_round_increment),
        selected = rule.incrementKg,
        unit = unit,
        hasDefault = false,
        onSelect = { onChange(rule.copy(incrementKg = it ?: rule.incrementKg)) },
    )
}

@Composable
private fun SettingLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(top = LiftLogDimens.spacingS), style = MaterialTheme.typography.labelLarge)
}

/** Null is the weight step of the exercise. */
@Composable
private fun ColumnScope.IncrementChips(
    label: String,
    selected: Double?,
    unit: WeightUnit,
    hasDefault: Boolean,
    onSelect: (Double?) -> Unit,
) {
    val text = SetText(unit)
    // Compared as shown: a stored 2.2679… kg is 5 lb.
    val sizes = (incrementsKg(unit) + listOfNotNull(selected)).distinctBy { text.weight(Weight(it)) }.sorted()
    val options = (if (hasDefault) listOf(null) else emptyList()) + sizes
    SettingLabel(label)
    Chips(
        options = options,
        selected = options.firstOrNull { it != null && selected != null && text.same(it, selected) },
        label = { size ->
            size?.let { text.weight(Weight(it)) } ?: stringResource(R.string.routine_rule_default_step)
        },
        onSelect = onSelect,
    )
}

private fun SetText.same(first: Double, second: Double) = weight(Weight(first)) == weight(Weight(second))

private fun incrementsKg(unit: WeightUnit): List<Double> = when (unit) {
    WeightUnit.KG -> IncrementsKg
    WeightUnit.LB -> IncrementsLb.map { Weight.of(it, WeightUnit.LB).kg }
}

@Composable
private fun ColumnScope.PercentChips(label: String, selected: Int, onSelect: (Int) -> Unit) {
    SettingLabel(label)
    Chips(
        options = (DeloadPercents + selected).distinct().sorted(),
        selected = selected,
        label = { stringResource(R.string.routine_rule_percent, it) },
        onSelect = onSelect,
    )
}

@Composable
private fun <T> Chips(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
        for (option in options) {
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/** "5 × 3, 6 × 2, 10 × 1". */
@Composable
internal fun stagesText(stages: List<Stage>): String =
    stages.map { stringResource(R.string.routine_stage, it.sets, it.reps) }.joinToString()

/** "Double progression", "Linear, +5 kg", "5/3/1, training max 100 kg". */
@Composable
internal fun progressionLabel(rule: ProgressionRule, unit: WeightUnit): String {
    val name = stringResource(rule.type.nameRes())
    val weight = when (rule) {
        is ProgressionRule.Linear -> rule.incrementKg
        is ProgressionRule.FiveThreeOne -> rule.trainingMaxKg
        else -> null
    }?.let { SetText(unit).weight(Weight(it)) }
    return when {
        rule is ProgressionRule.Stages -> stagesText(rule.stages)

        rule is ProgressionRule.FiveThreeOne && weight == null -> stringResource(
            R.string.routine_progression_no_max,
            name,
        )

        weight == null -> name

        rule is ProgressionRule.FiveThreeOne -> stringResource(R.string.routine_progression_max, name, weight)

        else -> stringResource(R.string.routine_progression_increment, name, weight)
    }
}

private fun ProgressionRuleType.nameRes(): Int = when (this) {
    ProgressionRuleType.DOUBLE_PROGRESSION -> R.string.routine_rule_double
    ProgressionRuleType.LINEAR -> R.string.routine_rule_linear
    ProgressionRuleType.STAGES -> R.string.routine_rule_stages
    ProgressionRuleType.FIVE_THREE_ONE -> R.string.routine_rule_531
    ProgressionRuleType.NONE -> R.string.routine_rule_none
}

private fun ProgressionRuleType.descriptionRes(): Int = when (this) {
    ProgressionRuleType.DOUBLE_PROGRESSION -> R.string.routine_rule_double_body
    ProgressionRuleType.LINEAR -> R.string.routine_rule_linear_body
    ProgressionRuleType.STAGES -> R.string.routine_rule_stages_body
    ProgressionRuleType.FIVE_THREE_ONE -> R.string.routine_rule_531_body
    ProgressionRuleType.NONE -> R.string.routine_rule_none_body
}
