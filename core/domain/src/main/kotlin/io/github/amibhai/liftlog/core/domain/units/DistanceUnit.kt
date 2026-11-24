package io.github.amibhai.liftlog.core.domain.units

import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import java.math.BigDecimal
import java.math.RoundingMode

private const val METERS_PER_KILOMETER = 1_000.0
private const val METERS_PER_MILE = 1_609.344
private const val METERS_PER_YARD = 0.9144

/** Units for distances, which are stored in metres and converted only for display, like weights. */
enum class DistanceUnit(private val metersPerUnit: Double) {
    METER(1.0),
    KILOMETER(METERS_PER_KILOMETER),
    YARD(METERS_PER_YARD),
    MILE(METERS_PER_MILE),
    ;

    fun toMeters(value: BigDecimal): Double = value.toDouble() * metersPerUnit

    /** Rounded to two decimals, so that whatever was typed is displayed back exactly. */
    fun display(meters: Double): BigDecimal {
        val rounded = BigDecimal.valueOf(meters / metersPerUnit).setScale(DECIMALS, RoundingMode.HALF_UP)
        return if (rounded.signum() == 0) BigDecimal.ZERO else rounded.stripTrailingZeros()
    }

    companion object {
        const val DECIMALS = 2

        /** Carries are short, cardio is long; the weight unit tells metric from imperial. */
        fun of(type: ExerciseType, unit: WeightUnit): DistanceUnit = when {
            type == ExerciseType.WEIGHT_DISTANCE && unit == WeightUnit.KG -> METER
            type == ExerciseType.WEIGHT_DISTANCE -> YARD
            unit == WeightUnit.KG -> KILOMETER
            else -> MILE
        }
    }
}
