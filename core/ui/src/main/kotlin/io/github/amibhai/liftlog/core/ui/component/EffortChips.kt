package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.workout.scale
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import java.math.BigDecimal

/** Rates the set that was just completed with one tap; tapping the chosen value takes it back (SPEC §5). */
@Composable
fun EffortChips(mode: EffortMode, selected: Double?, onSelect: (Double?) -> Unit, modifier: Modifier = Modifier) {
    val name = stringResource(if (mode == EffortMode.RIR) R.string.ui_rir else R.string.ui_rpe)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = LiftLogDimens.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "name") { Text(name, style = MaterialTheme.typography.labelLarge) }
        items(mode.scale, key = { it }) { value ->
            val description = stringResource(R.string.ui_effort_value, name, effortText(value))
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value.takeIf { it != selected }) },
                label = { Text(effortText(value)) },
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

/** "8.5", "2". */
fun effortText(value: Double): String = LiftLogFormat.decimal(BigDecimal.valueOf(value))
