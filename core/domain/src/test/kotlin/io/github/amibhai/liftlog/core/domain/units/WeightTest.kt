package io.github.amibhai.liftlog.core.domain.units

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class WeightTest {

    @Test
    fun `entering 45 lb displays back as exactly 45 lb`() {
        val weight = Weight.of(45.0, WeightUnit.LB)

        assertThat(weight.displayText(WeightUnit.LB)).isEqualTo("45")
    }

    @Test
    fun `weights are stored canonically in kilograms`() {
        assertThat(Weight.of(100.0, WeightUnit.KG).kg).isEqualTo(100.0)
        assertThat(Weight.of(1.0, WeightUnit.LB).kg).isEqualTo(0.45359237)
    }

    @Test
    fun `every quarter step from 0 to 1000 lb round-trips without drift`() {
        assertRoundTrips(WeightUnit.LB, maxValue = 1_000.0)
    }

    @Test
    fun `every quarter step from 0 to 500 kg round-trips without drift`() {
        assertRoundTrips(WeightUnit.KG, maxValue = 500.0)
    }

    @Test
    fun `two-decimal input round-trips in both units`() {
        for (unit in WeightUnit.entries) {
            for (hundredths in 0..20_000 step 7) {
                val typed = BigDecimal.valueOf(hundredths.toLong(), 2)
                val displayed = Weight.of(typed, unit).displayValue(unit)
                assertThat(displayed.compareTo(typed)).isEqualTo(0)
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
        "100, KG, LB, 220.46",
        "20, KG, LB, 44.09",
        "45, LB, KG, 20.41",
        "225, LB, KG, 102.06",
        "0, KG, LB, 0",
    )
    fun `converts between units rounded to two decimals`(
        value: Double,
        from: WeightUnit,
        to: WeightUnit,
        expected: String,
    ) {
        assertThat(Weight.of(value, from).displayText(to)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        "45, '45'",
        "177.5, '177.5'",
        "46.35, '46.35'",
        "0.5, '0.5'",
        "100.10, '100.1'",
    )
    fun `display text has no trailing zeros`(value: Double, expected: String) {
        assertThat(Weight.of(value, WeightUnit.KG).displayText(WeightUnit.KG)).isEqualTo(expected)
    }

    @Test
    fun `stepping in pounds adds exactly the step`() {
        val stepped = Weight.of(45.0, WeightUnit.LB).stepped(5.0, WeightUnit.LB)

        assertThat(stepped.displayText(WeightUnit.LB)).isEqualTo("50")
    }

    @Test
    fun `repeated stepping does not accumulate floating point error`() {
        var weight = Weight.of(20.0, WeightUnit.KG)
        repeat(40) { weight = weight.stepped(1.25, WeightUnit.KG) }

        assertThat(weight.displayText(WeightUnit.KG)).isEqualTo("70")
    }

    @Test
    fun `stepping down never goes below zero`() {
        val stepped = Weight.of(2.0, WeightUnit.KG).stepped(-2.5, WeightUnit.KG)

        assertThat(stepped).isEqualTo(Weight.ZERO)
    }

    @Test
    fun `weights compare by kilograms`() {
        assertThat(Weight.of(45.0, WeightUnit.LB)).isGreaterThan(Weight.of(20.0, WeightUnit.KG))
        assertThat(
            Weight.of(1.0, WeightUnit.KG) + Weight.of(2.0, WeightUnit.KG),
        ).isEqualTo(Weight.of(3.0, WeightUnit.KG))
    }

    private fun assertRoundTrips(unit: WeightUnit, maxValue: Double) {
        var quarters = 0
        while (quarters * QUARTER <= maxValue) {
            val typed = quarters * QUARTER
            val displayed = Weight.of(typed, unit).displayValue(unit)
            assertThat(displayed.compareTo(BigDecimal.valueOf(typed))).isEqualTo(0)
            quarters++
        }
    }

    private companion object {
        const val QUARTER = 0.25
    }
}
