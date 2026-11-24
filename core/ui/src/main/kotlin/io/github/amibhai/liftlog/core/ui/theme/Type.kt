package io.github.amibhai.liftlog.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/** Tabular figures: digits share one width, so weights, reps and timers never jitter as they change. */
private const val TABULAR_FIGURES = "tnum"

private val Base = Typography()

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)

internal val LiftLogTypography = Base.copy(
    displayLarge = Base.displayLarge.tabular(),
    displayMedium = Base.displayMedium.tabular(),
    displaySmall = Base.displaySmall.tabular(),
    headlineLarge = Base.headlineLarge.tabular(),
    headlineMedium = Base.headlineMedium.tabular(),
    headlineSmall = Base.headlineSmall.tabular(),
    titleLarge = Base.titleLarge.tabular(),
    titleMedium = Base.titleMedium.tabular(),
    titleSmall = Base.titleSmall.tabular(),
    bodyLarge = Base.bodyLarge.tabular(),
    bodyMedium = Base.bodyMedium.tabular(),
    bodySmall = Base.bodySmall.tabular(),
    labelLarge = Base.labelLarge.tabular(),
    labelMedium = Base.labelMedium.tabular(),
    labelSmall = Base.labelSmall.tabular(),
)
