package io.github.amibhai.liftlog.core.ui.component.chart

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Most steps between the labels of an axis. */
private const val MAX_STEPS = 5
private val NiceSteps = listOf(1.0, 2.0, 5.0, 10.0)

/** Without a spread to go by, a step is about this part of the value. */
private const val FLAT_STEP = 0.05

/** Finer steps could not be told apart by their labels. */
private const val LEAST_STEP = 0.1

/** The values an axis shows: whole steps of 1, 2 or 5, 10, 20 or 50, and so on. */
internal data class ValueAxis(val min: Double, val max: Double, val step: Double) {

    companion object {
        /** Close around the [values], so that small changes of large numbers stay visible. */
        fun around(values: List<Double>, wholeNumbers: Boolean): ValueAxis {
            val low = values.minOrNull() ?: 0.0
            val high = values.maxOrNull() ?: 1.0
            val spread = if (high > low) high - low else high * FLAT_STEP * MAX_STEPS
            val step = niceStep(spread, wholeNumbers)
            val min = floor(low / step) * step
            val max = ceil(high / step) * step
            // A flat line runs through the middle.
            return if (high > low) {
                ValueAxis(min, max, step)
            } else {
                ValueAxis((min - step).coerceAtLeast(0.0), max + step, step)
            }
        }

        fun fromZero(values: List<Double>, wholeNumbers: Boolean): ValueAxis {
            val high = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
            val step = niceStep(high, wholeNumbers)
            return ValueAxis(0.0, ceil(high / step) * step, step)
        }

        private fun niceStep(spread: Double, wholeNumbers: Boolean): Double {
            val least = (spread / MAX_STEPS).coerceAtLeast(if (wholeNumbers) 1.0 else LEAST_STEP)
            val magnitude = 10.0.pow(floor(log10(least)))
            return NiceSteps.map { it * magnitude }.first { it >= least }
        }
    }
}
