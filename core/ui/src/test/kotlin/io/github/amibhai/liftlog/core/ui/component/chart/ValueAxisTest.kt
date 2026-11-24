package io.github.amibhai.liftlog.core.ui.component.chart

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValueAxisTest {

    @Test
    fun theAxisEndsOnNiceNumbersAroundTheValues() {
        assertThat(ValueAxis.around(listOf(88.7, 110.8, 76.0), wholeNumbers = false))
            .isEqualTo(ValueAxis(min = 70.0, max = 120.0, step = 10.0))
    }

    @Test
    fun aFlatLineRunsThroughTheMiddle() {
        val axis = ValueAxis.around(listOf(60.0, 60.0), wholeNumbers = false)

        assertThat(axis).isEqualTo(ValueAxis(min = 55.0, max = 65.0, step = 5.0))
    }

    @Test
    fun wholeNumbersHaveNoHalfSteps() {
        assertThat(ValueAxis.fromZero(listOf(1.0, 3.0), wholeNumbers = true))
            .isEqualTo(ValueAxis(min = 0.0, max = 3.0, step = 1.0))
        assertThat(ValueAxis.around(listOf(8.0, 9.0), wholeNumbers = true).step).isEqualTo(1.0)
    }

    @Test
    fun largeValuesHaveFewLabels() {
        val axis = ValueAxis.fromZero(listOf(18_450.0), wholeNumbers = false)

        assertThat(axis).isEqualTo(ValueAxis(min = 0.0, max = 20_000.0, step = 5_000.0))
    }

    @Test
    fun thereIsAnAxisWithoutValuesToo() {
        assertThat(ValueAxis.fromZero(listOf(0.0, 0.0), wholeNumbers = true).max).isEqualTo(1.0)
        assertThat(ValueAxis.around(listOf(0.0), wholeNumbers = false).max).isGreaterThan(0.0)
    }
}
