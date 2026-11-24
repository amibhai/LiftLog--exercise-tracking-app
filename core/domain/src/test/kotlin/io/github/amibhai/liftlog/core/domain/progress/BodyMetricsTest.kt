package io.github.amibhai.liftlog.core.domain.progress

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class BodyMetricsTest {

    @Test
    fun `the trend averages the last seven days`() {
        val metrics = listOf(
            weight(day = 0, kg = 80.0),
            weight(day = 3, kg = 82.0),
            weight(day = 6, kg = 81.0),
            weight(day = 10, kg = 85.0),
        )

        val trend = movingAverage(metrics)

        assertThat(trend.map { it.value }).containsExactly(80.0, 81.0, 81.0, 83.0).inOrder()
    }

    @Test
    fun `weight shows in the chosen unit and keeps its exact value`() {
        val shown = displayValue(BodyMetricType.BODY_WEIGHT, 80.0, WeightUnit.LB)
        assertThat(shown).isEqualTo(BigDecimal("176.4"))

        val stored = canonicalValue(BodyMetricType.BODY_WEIGHT, 176.4, WeightUnit.LB)
        assertThat(displayValue(BodyMetricType.BODY_WEIGHT, stored, WeightUnit.LB)).isEqualTo(BigDecimal("176.4"))
    }

    @Test
    fun `lengths are centimeters with kilograms and inches with pounds`() {
        assertThat(displayValue(BodyMetricType.WAIST, 84.0, WeightUnit.KG)).isEqualTo(BigDecimal("84"))
        assertThat(displayValue(BodyMetricType.WAIST, 84.0, WeightUnit.LB)).isEqualTo(BigDecimal("33.1"))
        assertThat(canonicalValue(BodyMetricType.WAIST, 10.0, WeightUnit.LB)).isEqualTo(25.4)
        assertThat(LengthUnit.of(WeightUnit.LB).fromCentimeters(25.4)).isEqualTo(10.0)
    }

    @Test
    fun `a percentage has no unit`() {
        assertThat(displayValue(BodyMetricType.BODY_FAT, 15.25, WeightUnit.LB)).isEqualTo(BigDecimal("15.3"))
    }

    private fun weight(day: Int, kg: Double) =
        BodyMetric(BodyMetricId("$day"), BodyMetricType.BODY_WEIGHT, kg, TimeUnit.DAYS.toMillis(day.toLong()))
}
