package io.github.amibhai.liftlog.core.ui.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class NumberInputStateTest {

    private val weight = NumberInputState.start(BigDecimal("80"), maxDecimals = 2, maxValue = BigDecimal("999.99"))
    private val reps = NumberInputState.start(BigDecimal("8"), maxDecimals = 0, maxValue = BigDecimal("999"))

    @Test
    fun `first digit replaces the pre-filled value`() {
        assertThat(weight.digit(9).text).isEqualTo("9")
    }

    @Test
    fun `later digits append`() {
        assertThat(weight.digit(1).digit(0).digit(0).text).isEqualTo("100")
    }

    @Test
    fun `decimals are limited per field`() {
        val typed = weight.digit(2).decimal().digit(5).digit(5).digit(5)

        assertThat(typed.text).isEqualTo("2.55")
        assertThat(reps.decimal()).isEqualTo(reps)
    }

    @Test
    fun `decimal on a fresh field starts at zero`() {
        assertThat(weight.decimal().digit(5).text).isEqualTo("0.5")
    }

    @Test
    fun `values above the maximum are rejected`() {
        assertThat(reps.digit(9).digit(9).digit(9).digit(9).text).isEqualTo("999")
    }

    @Test
    fun `leading zero is replaced by the next digit`() {
        assertThat(weight.digit(0).digit(7).text).isEqualTo("7")
    }

    @Test
    fun `backspace clears a fresh field and deletes one character otherwise`() {
        assertThat(weight.backspace().text).isEmpty()
        assertThat(weight.digit(1).digit(2).backspace().text).isEqualTo("1")
    }

    @Test
    fun `steps are exact in the display unit and clamp at zero`() {
        val pounds = NumberInputState.start(BigDecimal("45"), maxDecimals = 2, maxValue = BigDecimal("999.99"))

        assertThat(pounds.step(BigDecimal("5")).text).isEqualTo("50")
        assertThat(weight.step(BigDecimal("2.5")).step(BigDecimal("2.5")).text).isEqualTo("85")
        assertThat(pounds.step(BigDecimal("-50")).text).isEqualTo("0")
    }

    @Test
    fun `after a step the next digit replaces the value`() {
        assertThat(weight.step(BigDecimal("2.5")).digit(6).text).isEqualTo("6")
    }

    @Test
    fun `same as previous takes the previous value`() {
        assertThat(weight.digit(1).set(BigDecimal("77.50")).text).isEqualTo("77.5")
    }

    @Test
    fun `reduce maps pad keys and ignores navigation`() {
        val step = BigDecimal("2.5")

        assertThat(weight.reduce(NumberPadKey.StepUp, step, null).text).isEqualTo("82.5")
        assertThat(weight.reduce(NumberPadKey.StepDown, step, null).text).isEqualTo("77.5")
        assertThat(weight.reduce(NumberPadKey.SameAsPrevious, step, BigDecimal("75")).text).isEqualTo("75")
        assertThat(weight.reduce(NumberPadKey.Next, step, null)).isEqualTo(weight)
        assertThat(weight.reduce(NumberPadKey.StepUp, null, null)).isEqualTo(weight)
    }
}
