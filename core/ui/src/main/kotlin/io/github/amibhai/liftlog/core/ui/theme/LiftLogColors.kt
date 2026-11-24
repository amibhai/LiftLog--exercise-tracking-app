package io.github.amibhai.liftlog.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private const val IDLE_SHADE = 0.14f
private const val SILHOUETTE_SHADE = 0.07f

/**
 * Colors for drawings on a card. They are mixed from the card's own color, because with dynamic color
 * any container role can be that same color, and the drawing would disappear.
 */
object LiftLogColors {

    /** Something without data, such as a day without training: quiet, but visible. */
    val idle: Color
        @Composable @ReadOnlyComposable
        get() = onCard(IDLE_SHADE)

    /** The body behind the muscles of the heatmap. */
    val silhouette: Color
        @Composable @ReadOnlyComposable
        get() = onCard(SILHOUETTE_SHADE)

    @Composable
    @ReadOnlyComposable
    private fun onCard(shade: Float): Color =
        MaterialTheme.colorScheme.run { lerp(surfaceContainerHighest, onSurface, shade) }
}
