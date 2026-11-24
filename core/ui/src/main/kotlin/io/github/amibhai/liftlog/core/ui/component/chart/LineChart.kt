package io.github.amibhai.liftlog.core.ui.component.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlin.math.max

private val ChartHeight = 200.dp
private val ColumnThickness = 14.dp
private val PointSize = 6.dp
private const val MAX_LABELS = 6

/** Room above and below the line, as a part of the values' spread. */
private const val PADDING = 0.1

/** A value of a chart; [label] goes under it. */
@Immutable
data class ChartPoint(val label: String, val value: Double)

/**
 * Values over time, oldest first, with an optional second line such as a trend (SPEC §8). TalkBack
 * reads [description] instead of the picture.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    valueText: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
    trend: List<Double>? = null,
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points, trend) {
        producer.runTransaction {
            lineModel {
                series(points.map { it.value })
                if (trend != null) series(trend)
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val range = remember(points, trend) { lineRange(points.map { it.value } + trend.orEmpty()) }
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(colors.primary)),
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.Point(
                                    rememberShapeComponent(fill = Fill(colors.primary), shape = CircleShape),
                                    PointSize,
                                ),
                            ),
                        ),
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(colors.tertiary)),
                        ),
                    ),
                    rangeProvider = range,
                ),
                startAxis = startAxis(valueText),
                bottomAxis = bottomAxis(points),
            ),
            modelProducer = producer,
            modifier = modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clearAndSetSemantics { contentDescription = description },
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}

/** Amounts per period, such as workouts per week (SPEC §8). TalkBack reads [description] instead. */
@Composable
fun ColumnChart(
    points: List<ChartPoint>,
    valueText: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        producer.runTransaction { columnModel { series(points.map { it.value }) } }
    }
    val color = MaterialTheme.colorScheme.primary
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        rememberLineComponent(
                            fill = Fill(color),
                            thickness = ColumnThickness,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        ),
                    ),
                    rangeProvider = remember { CartesianLayerRangeProvider.fixed(minY = 0.0) },
                ),
                startAxis = startAxis(valueText),
                bottomAxis = bottomAxis(points),
            ),
            modelProducer = producer,
            modifier = modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clearAndSetSemantics { contentDescription = description },
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}

@Composable
private fun startAxis(valueText: (Double) -> String): VerticalAxis<Axis.Position.Vertical.Start> {
    val text by rememberUpdatedState(valueText)
    return VerticalAxis.rememberStart(
        valueFormatter = remember { CartesianValueFormatter { _, value, _ -> text(value) } },
    )
}

/** Labels under some of the points, as many as fit. */
@Composable
private fun bottomAxis(points: List<ChartPoint>): HorizontalAxis<Axis.Position.Horizontal.Bottom> {
    val labels by rememberUpdatedState(points.map { it.label })
    val spacing = max(1, points.size / MAX_LABELS)
    return HorizontalAxis.rememberBottom(
        valueFormatter = remember { CartesianValueFormatter { _, value, _ -> labels.getOrNull(value.toInt()) ?: "·" } },
        itemPlacer = remember(spacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { spacing }) },
    )
}

/** Room around the values, so that small changes of large numbers stay visible. */
private fun lineRange(values: List<Double>): CartesianLayerRangeProvider {
    val low = values.minOrNull() ?: 0.0
    val high = values.maxOrNull() ?: 1.0
    val padding = max((high - low) * PADDING, high * PADDING / 2).coerceAtLeast(PADDING)
    return CartesianLayerRangeProvider.fixed(minY = (low - padding).coerceAtLeast(0.0), maxY = high + padding)
}
