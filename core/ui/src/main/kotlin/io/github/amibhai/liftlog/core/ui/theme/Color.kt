package io.github.amibhai.liftlog.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * LiftLog's own palette, used when dynamic color is off or unavailable (Android < 12):
 * charcoal surfaces, an electric-lime primary for "go" actions and completed sets, and a coral
 * tertiary reserved for PRs and celebrations. Text pairs meet WCAG AA (checked by ColorContrastTest).
 */

internal val LiftLogDarkColors = darkColorScheme(
    primary = Color(0xFFC6F24E),
    onPrimary = Color(0xFF1A2600),
    primaryContainer = Color(0xFF3A4D00),
    onPrimaryContainer = Color(0xFFDDFF8F),
    inversePrimary = Color(0xFF4E6700),
    secondary = Color(0xFFBACBA4),
    onSecondary = Color(0xFF253516),
    secondaryContainer = Color(0xFF3B4B2B),
    onSecondaryContainer = Color(0xFFD6E8BF),
    tertiary = Color(0xFFFFB4A1),
    onTertiary = Color(0xFF5F1606),
    tertiaryContainer = Color(0xFF7E2B19),
    onTertiaryContainer = Color(0xFFFFDAD2),
    background = Color(0xFF121411),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF121411),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF44483D),
    onSurfaceVariant = Color(0xFFC5C8BA),
    surfaceTint = Color(0xFFC6F24E),
    inverseSurface = Color(0xFFE3E3DC),
    inverseOnSurface = Color(0xFF2F312C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8F9285),
    outlineVariant = Color(0xFF44483D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383A35),
    surfaceDim = Color(0xFF121411),
    surfaceContainerLowest = Color(0xFF0D0F0C),
    surfaceContainerLow = Color(0xFF1A1C18),
    surfaceContainer = Color(0xFF1E201C),
    surfaceContainerHigh = Color(0xFF282B26),
    surfaceContainerHighest = Color(0xFF333530),
)

internal val LiftLogLightColors = lightColorScheme(
    primary = Color(0xFF4A6400),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6F24E),
    onPrimaryContainer = Color(0xFF141F00),
    inversePrimary = Color(0xFFADD534),
    secondary = Color(0xFF57624A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE7C8),
    onSecondaryContainer = Color(0xFF151E0C),
    tertiary = Color(0xFFA33A22),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD2),
    onTertiaryContainer = Color(0xFF3C0700),
    background = Color(0xFFFAFAF2),
    onBackground = Color(0xFF1A1C17),
    surface = Color(0xFFFAFAF2),
    onSurface = Color(0xFF1A1C17),
    surfaceVariant = Color(0xFFE1E4D5),
    onSurfaceVariant = Color(0xFF44483D),
    surfaceTint = Color(0xFF4A6400),
    inverseSurface = Color(0xFF2F312C),
    inverseOnSurface = Color(0xFFF1F1E9),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF75796C),
    outlineVariant = Color(0xFFC5C8BA),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAFAF2),
    surfaceDim = Color(0xFFDADBD3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F4EC),
    surfaceContainer = Color(0xFFEEEEE6),
    surfaceContainerHigh = Color(0xFFE9E9E1),
    surfaceContainerHighest = Color(0xFFE3E3DB),
)
