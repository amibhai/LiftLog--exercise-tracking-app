package io.github.amibhai.liftlog.feature.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** The layouts, by the space they need at least. */
internal enum class WidgetSize(val size: DpSize) {
    /** The button alone. */
    BUTTON(DpSize(110.dp, 40.dp)),

    /** The button and the streak next to it. */
    BAR(DpSize(210.dp, 40.dp)),

    /** Two lines above the button. */
    CARD(DpSize(110.dp, 125.dp)),

    /** With this week's workouts. */
    TALL_CARD(DpSize(110.dp, 165.dp)),
}
