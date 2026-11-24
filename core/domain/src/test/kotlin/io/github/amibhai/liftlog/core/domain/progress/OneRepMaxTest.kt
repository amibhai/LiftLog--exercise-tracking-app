package io.github.amibhai.liftlog.core.domain.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class OneRepMaxTest {

    @Test
    fun `one rep is the weight itself`() {
        for (formula in E1rmFormula.entries) {
            assertThat(estimateOneRepMax(100.0, 1, formula)).isEqualTo(100.0)
        }
    }

    @Test
    fun `epley adds a thirtieth of the weight for every rep`() {
        assertThat(estimateOneRepMax(90.0, 10, E1rmFormula.EPLEY)).isWithin(1e-9).of(120.0)
    }

    @Test
    fun `brzycki divides by 37 minus the reps`() {
        assertThat(estimateOneRepMax(100.0, 5, E1rmFormula.BRZYCKI)).isWithin(1e-9).of(112.5)
    }

    @Test
    fun `more than twelve reps give no estimate`() {
        assertThat(estimateOneRepMax(60.0, 12, E1rmFormula.EPLEY)).isNotNull()
        assertThat(estimateOneRepMax(60.0, 13, E1rmFormula.EPLEY)).isNull()
    }

    @Test
    fun `without weight or reps there is no estimate`() {
        assertThat(estimateOneRepMax(0.0, 5, E1rmFormula.EPLEY)).isNull()
        assertThat(estimateOneRepMax(60.0, 0, E1rmFormula.BRZYCKI)).isNull()
    }
}
