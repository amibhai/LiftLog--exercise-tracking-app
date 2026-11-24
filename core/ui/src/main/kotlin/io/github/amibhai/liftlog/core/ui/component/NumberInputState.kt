package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * What the number pad is editing, as typed text in the display unit (always '.' as separator).
 *
 * Right after a field is opened, the first digit replaces the shown value instead of appending to
 * it, so typing a new weight never needs a clear first.
 */
@Immutable
data class NumberInputState(
    val text: String,
    val maxDecimals: Int,
    val maxValue: BigDecimal,
    val replaceOnNextKey: Boolean,
) {
    val value: BigDecimal? get() = text.toBigDecimalOrNull()

    fun digit(digit: Int): NumberInputState {
        require(digit in 0..MAX_DIGIT) { "Not a digit: $digit" }
        val base = if (replaceOnNextKey) "" else text
        val candidate = if (base == "0") digit.toString() else base + digit
        if (!accepts(candidate)) return copy(replaceOnNextKey = false)
        return copy(text = candidate, replaceOnNextKey = false)
    }

    fun decimal(): NumberInputState = when {
        maxDecimals == 0 -> this
        replaceOnNextKey || text.isEmpty() -> copy(text = "0.", replaceOnNextKey = false)
        '.' in text -> this
        else -> copy(text = "$text.")
    }

    /** On a freshly opened field, backspace clears it; otherwise it removes the last character. */
    fun backspace(): NumberInputState =
        if (replaceOnNextKey) copy(text = "", replaceOnNextKey = false) else copy(text = text.dropLast(1))

    /** Adds [delta] (negative to subtract), clamped to 0..[maxValue]. */
    fun step(delta: BigDecimal): NumberInputState {
        val next = ((value ?: BigDecimal.ZERO) + delta).coerceIn(BigDecimal.ZERO, maxValue)
        return copy(text = next.toPlainText(), replaceOnNextKey = true)
    }

    /** "Same as previous": takes the given value as if it had been typed. */
    fun set(newValue: BigDecimal?): NumberInputState =
        copy(text = newValue?.coerceIn(BigDecimal.ZERO, maxValue)?.toPlainText().orEmpty(), replaceOnNextKey = true)

    private fun accepts(candidate: String): Boolean {
        val decimals = candidate.substringAfter('.', missingDelimiterValue = "").length
        val number = candidate.toBigDecimalOrNull() ?: return false
        return decimals <= maxDecimals && number <= maxValue
    }

    private fun BigDecimal.toPlainText(): String = if (signum() == 0) "0" else stripTrailingZeros().toPlainString()

    /**
     * Applies a pad key. [step] is the exercise's −/+ step in the display unit and [previous] the
     * value from last time; Next and Hide are navigation and leave the input unchanged.
     */
    fun reduce(key: NumberPadKey, step: BigDecimal?, previous: BigDecimal?): NumberInputState = when (key) {
        is NumberPadKey.Digit -> digit(key.value)
        NumberPadKey.Decimal -> decimal()
        NumberPadKey.Backspace -> backspace()
        NumberPadKey.StepUp -> if (step != null) step(step) else this
        NumberPadKey.StepDown -> if (step != null) step(step.negate()) else this
        NumberPadKey.SameAsPrevious -> if (previous != null) set(previous) else this
        NumberPadKey.Next, NumberPadKey.Hide -> this
    }

    companion object {
        private const val MAX_DIGIT = 9

        fun start(initial: BigDecimal?, maxDecimals: Int, maxValue: BigDecimal) = NumberInputState(
            text = initial?.let { if (it.signum() == 0) "0" else it.stripTrailingZeros().toPlainString() }.orEmpty(),
            maxDecimals = maxDecimals,
            maxValue = maxValue,
            replaceOnNextKey = true,
        )
    }
}
