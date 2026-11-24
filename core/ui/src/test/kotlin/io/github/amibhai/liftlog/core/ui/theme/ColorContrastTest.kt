package io.github.amibhai.liftlog.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** SPEC §13 "sufficient contrast": every text/background pair of both palettes meets WCAG AA. */
class ColorContrastTest {

    @Test
    fun `dark palette meets WCAG AA for text`() = assertTextContrast(LiftLogDarkColors)

    @Test
    fun `light palette meets WCAG AA for text`() = assertTextContrast(LiftLogLightColors)

    private fun assertTextContrast(scheme: ColorScheme) {
        val pairs = with(scheme) {
            listOf(
                "onPrimary/primary" to (onPrimary to primary),
                "onPrimaryContainer/primaryContainer" to (onPrimaryContainer to primaryContainer),
                "onSecondary/secondary" to (onSecondary to secondary),
                "onSecondaryContainer/secondaryContainer" to (onSecondaryContainer to secondaryContainer),
                "onTertiary/tertiary" to (onTertiary to tertiary),
                "onTertiaryContainer/tertiaryContainer" to (onTertiaryContainer to tertiaryContainer),
                "onError/error" to (onError to error),
                "onErrorContainer/errorContainer" to (onErrorContainer to errorContainer),
                "onSurface/surface" to (onSurface to surface),
                "onSurfaceVariant/surface" to (onSurfaceVariant to surface),
                "onSurfaceVariant/surfaceVariant" to (onSurfaceVariant to surfaceVariant),
                "onSurface/surfaceContainerHighest" to (onSurface to surfaceContainerHighest),
                "onSurfaceVariant/surfaceContainerHigh" to (onSurfaceVariant to surfaceContainerHigh),
                "primary/surface" to (primary to surface),
                "inverseOnSurface/inverseSurface" to (inverseOnSurface to inverseSurface),
            )
        }
        for ((name, colors) in pairs) {
            assertWithMessage(name).that(contrast(colors.first, colors.second)).isAtLeast(WCAG_AA_TEXT)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + RELATIVE_LUMINANCE_OFFSET) / (darker + RELATIVE_LUMINANCE_OFFSET).toDouble()
    }

    private companion object {
        const val WCAG_AA_TEXT = 4.5
        const val RELATIVE_LUMINANCE_OFFSET = 0.05f
    }
}
