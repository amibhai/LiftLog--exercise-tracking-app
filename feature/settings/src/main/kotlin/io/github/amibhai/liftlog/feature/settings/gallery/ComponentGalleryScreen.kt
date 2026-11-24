package io.github.amibhai.liftlog.feature.settings.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.ui.component.EffortChips
import io.github.amibhai.liftlog.core.ui.component.NumberInputState
import io.github.amibhai.liftlog.core.ui.component.NumberPad
import io.github.amibhai.liftlog.core.ui.component.NumberPadUiState
import io.github.amibhai.liftlog.core.ui.component.PadInput
import io.github.amibhai.liftlog.core.ui.component.RestTimerBar
import io.github.amibhai.liftlog.core.ui.component.RestTimerUiState
import io.github.amibhai.liftlog.core.ui.component.SetFieldUiModel
import io.github.amibhai.liftlog.core.ui.component.SetRow
import io.github.amibhai.liftlog.core.ui.component.SetRowAction
import io.github.amibhai.liftlog.core.ui.component.SetRowUiModel
import io.github.amibhai.liftlog.core.ui.component.SwitchRow
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.R
import java.math.BigDecimal
import io.github.amibhai.liftlog.core.ui.R as UiR

private const val REST_SECONDS = 120
private const val MILLIS_PER_SECOND = 1_000L

/** Debug-only: the design-system components on one page, for trying changes on a phone. */
@Composable
internal fun ComponentGalleryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
        ) {
            SetRows()
            Effort()
            Timer()
            Switches()
            Pad()
        }
    }
}

@Composable
private fun SetRows(modifier: Modifier = Modifier) {
    var rows by remember { mutableStateOf(sampleRows()) }
    Column(modifier) {
        for (row in rows) {
            SetRow(
                model = row,
                onAction = { action ->
                    rows = rows.map { if (it.id == action.setId) it.after(action) else it }
                },
                modifier = Modifier.padding(horizontal = LiftLogDimens.spacingS),
            )
        }
    }
}

@Composable
private fun Effort(modifier: Modifier = Modifier) {
    var rating by remember { mutableStateOf<Double?>(null) }
    EffortChips(mode = EffortMode.RPE, selected = rating, onSelect = { rating = it }, modifier = modifier)
}

@Composable
private fun Timer(modifier: Modifier = Modifier) {
    var end by remember { mutableStateOf(System.currentTimeMillis() + REST_SECONDS * MILLIS_PER_SECOND) }
    RestTimerBar(
        state = RestTimerUiState(end, REST_SECONDS),
        onAdjust = { end += it * MILLIS_PER_SECOND },
        onSkip = { end = System.currentTimeMillis() },
        modifier = modifier.padding(horizontal = LiftLogDimens.spacingS),
    )
}

@Composable
private fun Switches(modifier: Modifier = Modifier) {
    var isOn by remember { mutableStateOf(true) }
    SwitchRow(
        title = stringResource(R.string.settings_dynamic_color),
        checked = isOn,
        onCheckedChange = { isOn = it },
        modifier = modifier,
        supportingText = stringResource(R.string.settings_dynamic_color_body),
    )
}

@Composable
private fun Pad(modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf<PadInput>(NumberInputState.start(BigDecimal("80"), 2, BigDecimal("999"))) }
    NumberPad(
        state = NumberPadUiState(
            fieldLabel = stringResource(UiR.string.ui_field_weight),
            valueText = input.display,
            stepLabel = "2.5",
            allowsDecimal = true,
            canUsePrevious = true,
            isLastField = false,
        ),
        onKey = { input = input.reduce(it, step = BigDecimal("2.5"), previous = BigDecimal("77.5")) },
        modifier = modifier,
    )
}

private fun SetRowUiModel.after(action: SetRowAction): SetRowUiModel = when (action) {
    is SetRowAction.ToggleComplete -> copy(isCompleted = !isCompleted)
    is SetRowAction.ChangeType -> copy(type = action.type)
    else -> this
}

private fun sampleRows() = listOf(SetType.WARMUP, SetType.WORKING, SetType.DROP, SetType.FAILURE).map { type ->
    SetRowUiModel(
        id = type.name,
        type = type,
        workingSetNumber = 1,
        previous = "77.5 × 8",
        fields = listOf(
            SetFieldUiModel(SetField.WEIGHT, "80", "kg", isGhost = type != SetType.WARMUP, note = "+2.5 kg"),
            SetFieldUiModel(SetField.REPS, "8", null, isGhost = type != SetType.WARMUP),
        ),
        isCompleted = type == SetType.WARMUP,
    )
}
