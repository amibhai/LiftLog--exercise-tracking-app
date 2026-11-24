package io.github.amibhai.liftlog

import android.content.Intent
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
import io.github.amibhai.liftlog.core.ui.launch.LaunchAction
import io.github.amibhai.liftlog.core.ui.theme.LiftLogTheme
import io.github.amibhai.liftlog.core.ui.theme.isDarkTheme
import io.github.amibhai.liftlog.feature.onboarding.OnboardingRoute
import io.github.amibhai.liftlog.ui.LiftLogApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    private var isDark = true

    /** Read again by the system bars after every configuration change, so it must not be a fixed value. */
    private val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // After a rotation, or from the recent apps, it was carried out before.
        val isFromRecents = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !isFromRecents) LaunchAction.of(intent)?.let(viewModel::onLaunch)
        // Keep the splash until settings are read, so the first frame already uses the chosen theme.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge(barStyle, barStyle)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val target by viewModel.target.collectAsStateWithLifecycle()
            val ready = uiState as? MainUiState.Ready ?: return@setContent
            val settings = ready.settings
            val darkTheme = settings.themeMode.isDarkTheme()
            // System bar icons follow the app theme, which can differ from the system theme.
            DisposableEffect(darkTheme) {
                isDark = darkTheme
                enableEdgeToEdge(barStyle, barStyle)
                onDispose {}
            }
            LiftLogTheme(darkTheme = darkTheme, dynamicColor = settings.useDynamicColor) {
                if (ready.showOnboarding) {
                    OnboardingRoute(onFinish = viewModel::onOnboardingFinished)
                } else {
                    LiftLogApp(
                        appVersion = BuildConfig.VERSION_NAME,
                        showDeveloperOptions = BuildConfig.DEBUG,
                        target = target,
                        onOpenTarget = viewModel::onTargetOpened,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LaunchAction.of(intent)?.let(viewModel::onLaunch)
    }
}
