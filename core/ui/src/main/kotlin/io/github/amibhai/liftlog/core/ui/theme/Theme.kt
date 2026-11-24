package io.github.amibhai.liftlog.core.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import io.github.amibhai.liftlog.core.domain.model.ThemeMode

/**
 * Material 3 Expressive theme (stable in material3 1.5) with expressive motion. Dark is the
 * default; [dynamicColor] uses the wallpaper palette on Android 12+ and LiftLog's own otherwise.
 */
@Composable
fun LiftLogTheme(darkTheme: Boolean = true, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = liftLogColorScheme(LocalContext.current, darkTheme, dynamicColor),
        motionScheme = MotionScheme.expressive(),
        typography = LiftLogTypography,
        content = content,
    )
}

/** The colors of [LiftLogTheme], for the widget, which is drawn without a Compose theme. */
fun liftLogColorScheme(context: Context, darkTheme: Boolean, dynamicColor: Boolean): ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    darkTheme -> LiftLogDarkColors

    else -> LiftLogLightColors
}

/** Resolves the user's theme setting against the system setting. */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}
