package io.github.amibhai.liftlog.core.domain.progress

import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.model.ProgressPhotoId
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

/** How a body metric is measured. */
enum class MetricKind { WEIGHT, LENGTH, PERCENT }

/** What can be measured (SPEC §8). Names are stored in the database. */
enum class BodyMetricType(val kind: MetricKind) {
    BODY_WEIGHT(MetricKind.WEIGHT),
    BODY_FAT(MetricKind.PERCENT),
    NECK(MetricKind.LENGTH),
    SHOULDERS(MetricKind.LENGTH),
    CHEST(MetricKind.LENGTH),
    WAIST(MetricKind.LENGTH),
    HIPS(MetricKind.LENGTH),
    UPPER_ARM(MetricKind.LENGTH),
    FOREARM(MetricKind.LENGTH),
    THIGH(MetricKind.LENGTH),
    CALF(MetricKind.LENGTH),
}

/** One measurement. [value] is canonical: kg, cm or percent. */
data class BodyMetric(
    val id: BodyMetricId,
    val type: BodyMetricType,
    val value: Double,
    val measuredAt: Long,
    val notes: String? = null,
)

/** A progress photo: [path] is where the picture is on the phone, in the app's private storage. */
data class ProgressPhoto(val id: ProgressPhotoId, val takenAt: Long, val path: String)

/** Lengths are shown in centimeters with kilograms and in inches with pounds. */
enum class LengthUnit(private val centimetersPerUnit: Double) {
    CENTIMETER(1.0),
    INCH(CENTIMETERS_PER_INCH),
    ;

    fun fromCentimeters(cm: Double): Double = cm / centimetersPerUnit

    fun toCentimeters(value: Double): Double = value * centimetersPerUnit

    companion object {
        fun of(unit: WeightUnit): LengthUnit = if (unit == WeightUnit.KG) CENTIMETER else INCH
    }
}

/** A point of the trend line. */
data class TrendPoint(val at: Long, val value: Double)

/** The average of the last [days] days up to each measurement, oldest first (SPEC §8: trend line). */
fun movingAverage(metrics: List<BodyMetric>, days: Int = TREND_DAYS): List<TrendPoint> {
    val sorted = metrics.sortedBy { it.measuredAt }
    val span = TimeUnit.DAYS.toMillis(days.toLong())
    return sorted.map { metric ->
        val window = sorted.filter { it.measuredAt in (metric.measuredAt - span + 1)..metric.measuredAt }
        TrendPoint(metric.measuredAt, window.sumOf { it.value } / window.size)
    }
}

/** A metric value as it is shown: rounded to one decimal in the unit in use. */
fun displayValue(type: BodyMetricType, value: Double, unit: WeightUnit): BigDecimal {
    val converted = when (type.kind) {
        MetricKind.WEIGHT -> value / unit.kilogramsPerUnit
        MetricKind.LENGTH -> LengthUnit.of(unit).fromCentimeters(value)
        MetricKind.PERCENT -> value
    }
    return BigDecimal.valueOf(converted).setScale(METRIC_DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros()
        .let { if (it.scale() < 0) it.setScale(0) else it }
}

/** The canonical value of what was typed in the unit in use. */
fun canonicalValue(type: BodyMetricType, shown: Double, unit: WeightUnit): Double = when (type.kind) {
    MetricKind.WEIGHT -> shown * unit.kilogramsPerUnit
    MetricKind.LENGTH -> LengthUnit.of(unit).toCentimeters(shown)
    MetricKind.PERCENT -> shown
}

private const val CENTIMETERS_PER_INCH = 2.54
private const val TREND_DAYS = 7
private const val METRIC_DECIMALS = 1
