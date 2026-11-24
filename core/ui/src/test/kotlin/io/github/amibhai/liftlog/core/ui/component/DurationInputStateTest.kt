package io.github.amibhai.liftlog.core.ui.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class DurationInputStateTest {

    @Test
    fun `digits fill minutes and seconds from the right`() {
        var input: PadInput = DurationInputState.start(null)
        val shown = listOf(1, 3, 0).map { digit ->
            input = input.reduce(NumberPadKey.Digit(digit), step = null, previous = null)
            input.display
        }

        assertThat(shown).containsExactly("0:01", "0:13", "1:30").inOrder()
        assertThat(input.value).isEqualTo(BigDecimal(90))
    }

    @Test
    fun `the first digit replaces the shown time`() {
        val input = DurationInputState.start(BigDecimal(90)).reduce(NumberPadKey.Digit(5), null, null)

        assertThat(DurationInputState.start(BigDecimal(90)).display).isEqualTo("1:30")
        assertThat(input.display).isEqualTo("0:05")
    }

    @Test
    fun `ninety seconds can be typed as 90 or as 1 30`() {
        val ninety = DurationInputState.start(null).type(9, 0)

        assertThat(ninety.display).isEqualTo("0:90")
        assertThat(ninety.value).isEqualTo(BigDecimal(90))
    }

    @Test
    fun `hours are possible but not more than six digits`() {
        val input = DurationInputState.start(null).type(1, 0, 2, 0, 3)

        assertThat(input.display).isEqualTo("1:02:03")
        assertThat(input.value).isEqualTo(BigDecimal(3_723))
        assertThat(input.type(4, 5).display).isEqualTo("10:20:34")
    }

    @Test
    fun `backspace removes the last digit and clears a fresh field`() {
        assertThat(DurationInputState.start(null).type(1, 3, 0).reduce(NumberPadKey.Backspace, null, null).display)
            .isEqualTo("0:13")
        val cleared = DurationInputState.start(BigDecimal(90)).reduce(NumberPadKey.Backspace, null, null)
        assertThat(cleared.value).isNull()
        assertThat(cleared.display).isEmpty()
    }

    @Test
    fun `steps and the previous time work like for numbers`() {
        val step = BigDecimal(5)
        val start = DurationInputState.start(BigDecimal(58))

        assertThat(start.reduce(NumberPadKey.StepUp, step, null).display).isEqualTo("1:03")
        assertThat(start.reduce(NumberPadKey.StepDown, BigDecimal(60), null).value).isEqualTo(BigDecimal.ZERO)
        assertThat(start.reduce(NumberPadKey.SameAsPrevious, step, BigDecimal(120)).display).isEqualTo("2:00")
    }

    private fun PadInput.type(vararg digits: Int): PadInput =
        digits.fold(this) { input, digit -> input.reduce(NumberPadKey.Digit(digit), null, null) }
}
