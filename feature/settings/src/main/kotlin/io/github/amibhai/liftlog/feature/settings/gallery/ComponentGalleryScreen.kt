package io.github.amibhai.liftlog.feature.settings.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.NumberPad
import io.github.amibhai.liftlog.core.ui.component.NumberPadUiState
import io.github.amibhai.liftlog.core.ui.component.RestTimerBar
import io.github.amibhai.liftlog.core.ui.component.SetRow
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.R
import io.github.amibhai.liftlog.core.ui.R as UiR

/**
 * Debug-only playground for the Phase 0 logging controls: set rows, the docked number pad and the
 * rest timer, wired together like the real workout screen will be. Nothing here is saved.
 */
@Composable
internal fun ComponentGalleryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(GalleryState()) }
    val unitLabel = stringResource(state.unit.labelRes())
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LiftLogIcons.ArrowBack),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
        bottomBar = { GalleryControls(state = state, unitLabel = unitLabel, onStateChange = { state = it }) },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXs),
        ) {
            item(key = "intro") { Intro(unit = state.unit, onUnitChange = { state = state.withUnit(it) }) }
            state.exercises.forEachIndexed { exerciseIndex, exercise ->
                item(key = "header-${exercise.name}") {
                    Column(
                        Modifier.padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingS),
                    ) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(exercise.type.labelRes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(exercise.sets, key = { _, set -> set.id }) { setIndex, _ ->
                    SetRow(
                        model = state.rowModel(exerciseIndex, setIndex, unitLabel),
                        onAction = { action ->
                            state = state.handle(action, exerciseIndex, setIndex, System.currentTimeMillis())
                        },
                        modifier = Modifier.padding(horizontal = LiftLogDimens.spacingS),
                    )
                }
            }
        }
    }
}

@Composable
private fun Intro(unit: WeightUnit, onUnitChange: (WeightUnit) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(LiftLogDimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        Text(stringResource(R.string.gallery_intro), style = MaterialTheme.typography.bodyMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            WeightUnit.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == unit,
                    onClick = { onUnitChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = WeightUnit.entries.size),
                    label = { Text(stringResource(option.labelRes())) },
                )
            }
        }
        Text(
            text = stringResource(R.string.gallery_unit_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GalleryControls(
    state: GalleryState,
    unitLabel: String,
    onStateChange: (GalleryState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ref = state.editing
    val input = state.input
    Column(
        modifier = modifier
            .fillMaxWidth()
            // The number pad pads itself above the navigation bar; the timer alone needs it here.
            .then(if (ref == null) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = LiftLogDimens.spacingS),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        state.restTimer?.let { timer ->
            RestTimerBar(
                state = timer,
                onAdjust = { onStateChange(state.adjustRest(it)) },
                onSkip = { onStateChange(state.skipRest()) },
            )
        }
        if (ref != null && input != null) {
            NumberPad(
                state = NumberPadUiState(
                    fieldLabel = fieldLabel(ref.field, unitLabel),
                    valueText = input.text.replace('.', LiftLogFormat.decimalSeparator()),
                    stepLabel = LiftLogFormat.decimal(state.step(ref)),
                    allowsDecimal = input.maxDecimals > 0,
                    canUsePrevious = state.previousValue(ref) != null,
                    isLastField = state.nextField(ref) == null,
                ),
                onKey = { onStateChange(state.press(it)) },
            )
        }
    }
}

@Composable
private fun fieldLabel(field: SetField, unitLabel: String): String = when (field) {
    SetField.WEIGHT -> stringResource(
        R.string.gallery_field_with_unit,
        stringResource(UiR.string.ui_field_weight),
        unitLabel,
    )

    SetField.REPS -> stringResource(UiR.string.ui_field_reps)

    SetField.DURATION -> stringResource(
        R.string.gallery_field_with_unit,
        stringResource(UiR.string.ui_field_duration),
        stringResource(R.string.gallery_seconds),
    )

    SetField.DISTANCE -> stringResource(UiR.string.ui_field_distance)
}
