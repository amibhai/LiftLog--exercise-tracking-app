package io.github.amibhai.liftlog.core.ui.component

import java.math.BigDecimal

/** What the number pad is editing: a number or a time. */
interface PadInput {
    /** The value as typed so far, with '.' as decimal separator; empty when there is none. */
    val display: String

    /** The value in the field's unit (seconds for a time); null when empty. */
    val value: BigDecimal?

    /** Applies a key. [step] and [previous] are in the unit of [value]. */
    fun reduce(key: NumberPadKey, step: BigDecimal?, previous: BigDecimal?): PadInput
}
