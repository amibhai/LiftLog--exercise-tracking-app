package io.github.amibhai.liftlog.core.ui.format

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.progress.ProgressMetric
import io.github.amibhai.liftlog.core.domain.units.DistanceUnit
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

private const val CHART_DECIMALS = 1

/** Values of a progress chart, in the unit an exercise is shown in (SPEC §8). */
@Immutable
data class MetricText(private val type: ExerciseType, private val unit: WeightUnit) {

    /** [value] as a chart shows it: kg become the chosen unit, meters the unit of the exercise. */
    fun displayed(metric: ProgressMetric, value: Double): Double = when (metric) {
        ProgressMetric.E1RM, ProgressMetric.TOP_WEIGHT, ProgressMetric.VOLUME -> value / unit.kilogramsPerUnit
        ProgressMetric.DISTANCE -> DistanceUnit.of(type, unit).display(value).toDouble()
        ProgressMetric.REPS, ProgressMetric.DURATION -> value
    }

    /** For an axis, without a unit: "102.5", "8", "1:30". */
    fun axis(metric: ProgressMetric, displayed: Double): String = when (metric) {
        ProgressMetric.DURATION -> LiftLogFormat.duration(displayed.roundToInt())
        ProgressMetric.VOLUME -> LiftLogFormat.decimal(rounded(displayed, 0))
        else -> LiftLogFormat.decimal(rounded(displayed, CHART_DECIMALS))
    }

    /** With its unit, for a headline: "102.5 kg", "8", "1:30", "5.3 km". */
    fun label(metric: ProgressMetric, displayed: Double): String = when (metric) {
        ProgressMetric.E1RM, ProgressMetric.TOP_WEIGHT, ProgressMetric.VOLUME ->
            axis(metric, displayed) + " " + unit.symbol

        ProgressMetric.DISTANCE -> axis(metric, displayed) + " " + DistanceUnit.of(type, unit).symbol

        ProgressMetric.REPS, ProgressMetric.DURATION -> axis(metric, displayed)
    }

    private fun rounded(value: Double, decimals: Int): BigDecimal =
        BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP)
}
