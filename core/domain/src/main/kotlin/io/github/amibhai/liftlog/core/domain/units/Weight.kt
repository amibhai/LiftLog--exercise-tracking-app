package io.github.amibhai.liftlog.core.domain.units

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A weight stored canonically in kilograms (SPEC §2).
 *
 * Convert only for display. Display values are rounded to [DISPLAY_DECIMALS] decimals of the
 * active unit, never to a plate step, so whatever the user typed (at most two decimals) is
 * displayed back exactly: 45 lb stays 45 lb.
 */
@JvmInline
value class Weight(val kg: Double) : Comparable<Weight> {

    init {
        require(kg.isFinite()) { "Weight must be finite, was $kg" }
    }

    /** Exact, unrounded value in [unit]. Use for calculations, not for display. */
    fun valueIn(unit: WeightUnit): Double = kg / unit.kilogramsPerUnit

    /** Value in [unit] rounded to display precision, without trailing zeros. */
    fun displayValue(unit: WeightUnit): BigDecimal {
        val rounded = BigDecimal.valueOf(valueIn(unit)).setScale(DISPLAY_DECIMALS, RoundingMode.HALF_UP)
        return if (rounded.signum() == 0) BigDecimal.ZERO else rounded.stripTrailingZeros()
    }

    /** [displayValue] as plain text with '.' as decimal separator; the UI localizes it. */
    fun displayText(unit: WeightUnit): String = displayValue(unit).toPlainString()

    /**
     * Adds [step] (negative to subtract) in the display unit, so 45 lb + 5 lb is exactly 50 lb.
     * The result is clamped at zero.
     */
    fun stepped(step: Double, unit: WeightUnit): Weight {
        val next = displayValue(unit).add(BigDecimal.valueOf(step))
        return if (next.signum() <= 0) ZERO else of(next, unit)
    }

    operator fun plus(other: Weight): Weight = Weight(kg + other.kg)

    operator fun minus(other: Weight): Weight = Weight(kg - other.kg)

    override fun compareTo(other: Weight): Int = kg.compareTo(other.kg)

    companion object {
        const val DISPLAY_DECIMALS = 2

        val ZERO = Weight(0.0)

        fun of(value: Double, unit: WeightUnit): Weight = Weight(value * unit.kilogramsPerUnit)

        fun of(value: BigDecimal, unit: WeightUnit): Weight = of(value.toDouble(), unit)
    }
}
