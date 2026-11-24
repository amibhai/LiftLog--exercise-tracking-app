package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.amibhai.liftlog.core.domain.plates.PlateCount
import io.github.amibhai.liftlog.core.domain.plates.PlateLoad
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import java.math.BigDecimal

private val BarWidth = 120.dp
private val BarHeight = 40.dp

/** Plates and gaps as parts of the drawing's height and width. */
private const val BAR_THICKNESS = 0.16f
private const val COLLAR_HEIGHT = 0.4f
private const val COLLAR_WIDTH = 0.04f
private const val PLATE_WIDTH = 0.075f
private const val PLATE_GAP = 0.015f
private const val SMALLEST_PLATE = 0.35f
private const val CORNER = 0.02f

/** Plates are drawn taller the heavier they are, up to this many kg. */
private const val TALLEST_PLATE_KG = 25.0

/** What to load on a bar, for the number pad (SPEC §5). */
@Immutable
data class PlateBarUiModel(val load: PlateLoad, val unit: WeightUnit)

/** One side of the bar with its plates, and the same in words: "25 + 15 + 1.25 per side". */
@Composable
fun PlateBar(model: PlateBarUiModel, modifier: Modifier = Modifier) {
    val text = SetText(model.unit)
    val perSide = model.load.perSide.flatMap { plate -> List(plate.count) { plateText(plate) } }
    val plates = if (perSide.isEmpty()) {
        stringResource(R.string.ui_plates_empty_bar)
    } else {
        stringResource(R.string.ui_plates_per_side, perSide.joinToString(" + "))
    }
    val details = if (model.load.isExact) {
        stringResource(R.string.ui_plates_bar, text.weight(model.load.bar))
    } else {
        stringResource(R.string.ui_plates_closest, text.weight(model.load.total))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$plates. $details" }
            .padding(horizontal = LiftLogDimens.spacingS),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarDrawing(model)
        Column(Modifier.weight(1f)) {
            Text(plates, style = MaterialTheme.typography.titleSmall)
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.run { if (model.load.isExact) onSurfaceVariant else error },
            )
        }
    }
}

@Composable
private fun BarDrawing(model: PlateBarUiModel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val heights = model.load.perSide.flatMap { plate -> List(plate.count) { plateHeight(plate, model.unit) } }
    Canvas(modifier.width(BarWidth).height(BarHeight)) {
        fun bar(left: Float, width: Float, height: Float, color: Color) = drawRoundRect(
            color = color,
            topLeft = Offset(left * size.width, size.height * (1 - height) / 2),
            size = Size(width * size.width, height * size.height),
            cornerRadius = CornerRadius(CORNER * size.width),
        )
        bar(left = 0f, width = 1f, height = BAR_THICKNESS, color = colors.outline)
        bar(left = COLLAR_WIDTH, width = COLLAR_WIDTH, height = COLLAR_HEIGHT, color = colors.outline)
        heights.forEachIndexed { index, height ->
            val left = 2 * COLLAR_WIDTH + PLATE_GAP + index * (PLATE_WIDTH + PLATE_GAP)
            bar(left, PLATE_WIDTH, height, if (index % 2 == 0) colors.primary else colors.tertiary)
        }
    }
}

private fun plateHeight(plate: PlateCount, unit: WeightUnit): Float {
    val part = plate.weight * unit.kilogramsPerUnit / TALLEST_PLATE_KG
    return (SMALLEST_PLATE + (1 - SMALLEST_PLATE) * part.toFloat()).coerceAtMost(1f)
}

private fun plateText(plate: PlateCount): String = LiftLogFormat.decimal(BigDecimal.valueOf(plate.weight))
