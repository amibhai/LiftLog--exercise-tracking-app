package io.github.amibhai.liftlog.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.feature.settings.SettingsRoute
import io.github.amibhai.liftlog.feature.settings.about.AboutRoute
import io.github.amibhai.liftlog.feature.settings.gallery.ComponentGalleryScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

@Serializable
data object AboutDestination

@Serializable
data object ComponentGalleryDestination

/**
 * Settings, About & attributions, and (debug builds only, [showDeveloperOptions]) the component
 * gallery for trying the logging controls on a phone.
 */
fun NavGraphBuilder.settingsGraph(
    appVersion: String,
    showDeveloperOptions: Boolean,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    composable<SettingsDestination> {
        SettingsRoute(
            showDeveloperOptions = showDeveloperOptions,
            onBack = onBack,
            onOpenAbout = onOpenAbout,
            onOpenGallery = onOpenGallery,
        )
    }
    composable<AboutDestination> { AboutRoute(appVersion = appVersion, onBack = onBack) }
    composable<ComponentGalleryDestination> { ComponentGalleryScreen(onBack = onBack) }
}
