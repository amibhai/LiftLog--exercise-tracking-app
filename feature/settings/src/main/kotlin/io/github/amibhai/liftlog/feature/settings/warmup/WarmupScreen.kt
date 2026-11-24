package io.github.amibhai.liftlog.feature.settings.warmup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WarmupStep
import io.github.amibhai.liftlog.core.domain.plates.warmupSets
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.NumberStepper
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.R
import io.github.amibhai.liftlog.feature.settings.SettingChange
import io.github.amibhai.liftlog.feature.settings.SettingsPage
import io.github.amibhai.liftlog.feature.settings.SettingsUiState
import io.github.amibhai.liftlog.feature.settings.SettingsViewModel

private const val PERCENT_STEP = 5
private const val MAX_PERCENT = 95
private const val MAX_REPS = 20
private const val MAX_STEPS = 8
private const val NEW_STEP_PERCENT = 10
private const val NEW_STEP_REPS = 3
private const val EXAMPLE_KG = 100.0
private const val EXAMPLE_LB = 225.0

/** Stands for any barbell exercise in the example. */
private val ExampleExercise = ExerciseInfo(ExerciseId(""), "", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)

@Composable
internal fun WarmupRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    (state as? SettingsUiState.Ready)?.let {
        WarmupScreen(settings = it.settings, onChange = viewModel::change, onBack = onBack)
    }
}

/** The steps of the warm-up generator (SPEC §5, §12), with an example of what they make. */
@Composable
internal fun WarmupScreen(
    settings: UserSettings,
    onChange: (SettingChange) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = settings.warmupSteps
    val change: (List<WarmupStep>) -> Unit = { onChange(SettingChange.Warmup(it)) }
    SettingsPage(title = stringResource(R.string.warmup_title), onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(LiftLogDimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            Text(
                text = stringResource(R.string.warmup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            steps.forEachIndexed { index, step ->
                StepRow(
                    number = index + 1,
                    step = step,
                    onChange = { new -> change(steps.mapIndexed { i, old -> if (i == index) new else old }) },
                    onRemove = { change(steps.filterIndexed { i, _ -> i != index }) },
                )
            }
            if (steps.size < MAX_STEPS) {
                FilledTonalButton(
                    onClick = { change(steps + newStep(steps)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LiftLogDimens.minTouchTarget),
                ) { Text(stringResource(R.string.warmup_add)) }
            }
            TextButton(onClick = { change(UserSettings().warmupSteps) }) { Text(stringResource(R.string.warmup_reset)) }
            Example(settings)
        }
    }
}

private fun newStep(steps: List<WarmupStep>): WarmupStep {
    val heaviest = steps.maxOfOrNull { it.percent } ?: 0
    return WarmupStep((heaviest + NEW_STEP_PERCENT).coerceAtMost(MAX_PERCENT), NEW_STEP_REPS)
}

@Composable
private fun StepRow(
    number: Int,
    step: WarmupStep,
    onChange: (WarmupStep) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        NumberStepper(
            value = step.percent / PERCENT_STEP,
            range = 0..MAX_PERCENT / PERCENT_STEP,
            onChange = { onChange(step.copy(percent = it * PERCENT_STEP)) },
            fewer = stringResource(R.string.warmup_lighter, number),
            more = stringResource(R.string.warmup_heavier, number),
            text = percentText(step.percent),
        )
        NumberStepper(
            value = step.reps,
            range = 1..MAX_REPS,
            onChange = { onChange(step.copy(reps = it)) },
            fewer = stringResource(R.string.warmup_fewer_reps, number),
            more = stringResource(R.string.warmup_more_reps, number),
            text = pluralStringResource(R.plurals.warmup_reps, step.reps, step.reps),
        )
        IconButton(onClick = onRemove) {
            Icon(painterResource(LiftLogIcons.Delete), stringResource(R.string.warmup_remove, number))
        }
    }
}

@Composable
private fun Example(settings: UserSettings, modifier: Modifier = Modifier) {
    val unit = settings.weightUnit
    val working = Weight.of(if (unit == WeightUnit.KG) EXAMPLE_KG else EXAMPLE_LB, unit)
    val text = SetText(unit)
    val sets = warmupSets(ExampleExercise, working, settings)
        .joinToString("\n") { text.summary(ExerciseType.WEIGHT_REPS, it) }
    LiftLogCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.warmup_example, text.weight(working)),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(sets.ifEmpty { stringResource(R.string.warmup_example_none) }, style = MaterialTheme.typography.bodyLarge)
    }
}

/** "Empty bar" or "40%". */
@Composable
internal fun percentText(percent: Int): String =
    if (percent == 0) stringResource(R.string.warmup_bar) else stringResource(R.string.warmup_percent, percent)
