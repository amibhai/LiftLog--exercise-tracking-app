package io.github.amibhai.liftlog

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.amibhai.liftlog.core.ui.theme.LiftLogTheme
import io.github.amibhai.liftlog.core.ui.theme.isDarkTheme
import io.github.amibhai.liftlog.ui.LiftLogApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until settings are read, so the first frame already uses the chosen theme.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val settings = (uiState as? MainUiState.Ready)?.settings ?: return@setContent
            val darkTheme = settings.themeMode.isDarkTheme()
            // System bar icons follow the app theme, which can differ from the system theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }
            LiftLogTheme(darkTheme = darkTheme, dynamicColor = settings.useDynamicColor) {
                LiftLogApp(appVersion = BuildConfig.VERSION_NAME, showDeveloperOptions = BuildConfig.DEBUG)
            }
        }
    }
}
