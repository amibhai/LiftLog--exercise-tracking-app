package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import java.math.BigDecimal

/** A time typed like on a microwave: 1, 3, 0 is 1:30. */
@Immutable
data class DurationInputState(private val digits: String, private val replaceOnNextKey: Boolean) : PadInput {

    override val value: BigDecimal?
        get() = digits.takeIf { it.isNotEmpty() }?.padStart(MAX_DIGITS, '0')?.chunked(2)?.map(String::toInt)
            ?.let { (hours, minutes, seconds) ->
                BigDecimal((hours * MINUTES_PER_HOUR + minutes) * SECONDS_PER_MINUTE + seconds)
            }

    override val display: String
        get() {
            if (digits.isEmpty()) return ""
            val (hours, minutes, seconds) = digits.padStart(MAX_DIGITS, '0').chunked(2)
            val long = digits.length > DIGITS_WITHOUT_HOURS
            return if (long) "${hours.toInt()}:$minutes:$seconds" else "${minutes.toInt()}:$seconds"
        }

    override fun reduce(key: NumberPadKey, step: BigDecimal?, previous: BigDecimal?): PadInput = when (key) {
        is NumberPadKey.Digit -> digit(key.value)

        NumberPadKey.Backspace -> copy(
            digits = if (replaceOnNextKey) "" else digits.dropLast(1),
            replaceOnNextKey = false,
        )

        NumberPadKey.StepUp -> stepped(step)

        NumberPadKey.StepDown -> stepped(step?.negate())

        NumberPadKey.SameAsPrevious -> if (previous != null) start(previous) else this

        NumberPadKey.Decimal, NumberPadKey.Next, NumberPadKey.Hide -> this
    }

    private fun digit(digit: Int): DurationInputState {
        val next = ((if (replaceOnNextKey) "" else digits) + digit).trimStart('0')
        return if (next.length > MAX_DIGITS) this else copy(digits = next, replaceOnNextKey = false)
    }

    private fun stepped(delta: BigDecimal?): DurationInputState =
        if (delta == null) this else start(((value ?: BigDecimal.ZERO) + delta).max(BigDecimal.ZERO))

    companion object {
        private const val MAX_DIGITS = 6
        private const val DIGITS_WITHOUT_HOURS = 4
        private const val MINUTES_PER_HOUR = 60
        private const val SECONDS_PER_MINUTE = 60

        fun start(seconds: BigDecimal?) = DurationInputState(
            digits = seconds?.let {
                LiftLogFormat.duration(
                    it.toInt(),
                ).filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
            }
                .orEmpty(),
            replaceOnNextKey = true,
        )
    }
}
