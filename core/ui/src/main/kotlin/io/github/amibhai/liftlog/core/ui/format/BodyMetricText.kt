package io.github.amibhai.liftlog.core.ui.format

import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.LengthUnit
import io.github.amibhai.liftlog.core.domain.progress.MetricKind
import io.github.amibhai.liftlog.core.domain.progress.displayValue
import io.github.amibhai.liftlog.core.domain.units.WeightUnit

/** "81.5 kg", "33.1 in", "15 %": a measurement in the unit in use. */
fun metricValueText(type: BodyMetricType, value: Double, unit: WeightUnit): String =
    LiftLogFormat.decimal(displayValue(type, value, unit)) + " " + type.unitText(unit)

/** What a measurement is written in. */
fun BodyMetricType.unitText(unit: WeightUnit): String = when (kind) {
    MetricKind.WEIGHT -> unit.symbol

    MetricKind.LENGTH -> when (LengthUnit.of(unit)) {
        LengthUnit.CENTIMETER -> "cm"
        LengthUnit.INCH -> "in"
    }

    MetricKind.PERCENT -> "%"
}
