package io.github.amibhai.liftlog.feature.settings.plates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.NumberStepper
import io.github.amibhai.liftlog.core.ui.component.TextInputDialog
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.symbol
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.R
import io.github.amibhai.liftlog.feature.settings.SettingChange
import io.github.amibhai.liftlog.feature.settings.SettingsPage
import io.github.amibhai.liftlog.feature.settings.SettingsUiState
import io.github.amibhai.liftlog.feature.settings.SettingsViewModel
import io.github.amibhai.liftlog.feature.settings.weightText
import java.math.BigDecimal

/** More pairs than this are as good as unlimited. */
private const val MOST_PAIRS = 10
private const val UNLIMITED = MOST_PAIRS + 1
private val MaxPlate = BigDecimal(200)

@Composable
internal fun PlatesRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    (state as? SettingsUiState.Ready)?.let {
        PlatesScreen(settings = it.settings, onChange = viewModel::change, onBack = onBack)
    }
}

/** The plates there are, for the plate calculator and for rounding (SPEC §2). */
@Composable
internal fun PlatesScreen(
    settings: UserSettings,
    onChange: (SettingChange) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isAdding by rememberSaveable { mutableStateOf(false) }
    val unit = settings.weightUnit
    val plates = settings.plates(unit).sortedByDescending { it.weight }
    SettingsPage(title = stringResource(R.string.plates_title), onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(LiftLogDimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            Text(
                text = stringResource(R.string.plates_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (plate in plates) {
                PlateRow(
                    plate = plate,
                    unit = unit,
                    onChange = { new -> onChange(SettingChange.Plates(plates.map { if (it == plate) new else it })) },
                    onRemove = { onChange(SettingChange.Plates(plates - plate)) },
                )
            }
            FilledTonalButton(
                onClick = { isAdding = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LiftLogDimens.minTouchTarget),
            ) { Text(stringResource(R.string.plates_add)) }
            TextButton(onClick = { onChange(SettingChange.Plates(UserSettings().plates(unit))) }) {
                Text(stringResource(R.string.plates_reset))
            }
        }
    }
    if (isAdding) {
        AddPlateDialog(
            unit = unit,
            onAdd = { weight ->
                isAdding = false
                if (plates.none { it.weight == weight }) onChange(SettingChange.Plates(plates + Plate(weight)))
            },
            onDismiss = { isAdding = false },
        )
    }
}

@Composable
private fun PlateRow(
    plate: Plate,
    unit: WeightUnit,
    onChange: (Plate) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = weightText(plate.weight, unit)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberStepper(
                value = plate.pairs ?: UNLIMITED,
                range = 0..UNLIMITED,
                onChange = { onChange(plate.copy(pairs = it.takeIf { pairs -> pairs <= MOST_PAIRS })) },
                fewer = stringResource(R.string.plates_fewer, name),
                more = stringResource(R.string.plates_more, name),
                text = pairsText(plate.pairs),
            )
            IconButton(onClick = onRemove) {
                Icon(painterResource(LiftLogIcons.Delete), stringResource(R.string.plates_remove, name))
            }
        }
    }
}

@Composable
private fun pairsText(pairs: Int?): String = when (pairs) {
    null -> stringResource(R.string.plates_unlimited)
    0 -> stringResource(R.string.plates_none)
    else -> pluralStringResource(R.plurals.plates_pairs, pairs, pairs)
}

@Composable
private fun AddPlateDialog(unit: WeightUnit, onAdd: (Double) -> Unit, onDismiss: () -> Unit) {
    TextInputDialog(
        title = stringResource(R.string.plates_add),
        initial = "",
        label = stringResource(R.string.plates_weight, unit.symbol),
        keyboardType = KeyboardType.Decimal,
        onConfirm = { text ->
            val weight = text.replace(LiftLogFormat.decimalSeparator(), '.').toBigDecimalOrNull()
            if (weight != null && weight.signum() > 0 && weight <= MaxPlate) onAdd(weight.toDouble()) else onDismiss()
        },
        onDismiss = onDismiss,
    )
}
