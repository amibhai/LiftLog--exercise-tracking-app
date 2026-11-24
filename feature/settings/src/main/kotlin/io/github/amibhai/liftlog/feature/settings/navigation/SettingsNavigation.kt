package io.github.amibhai.liftlog.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.feature.settings.SettingsNavigation
import io.github.amibhai.liftlog.feature.settings.SettingsRoute
import io.github.amibhai.liftlog.feature.settings.about.AboutRoute
import io.github.amibhai.liftlog.feature.settings.data.DataRoute
import io.github.amibhai.liftlog.feature.settings.gallery.ComponentGalleryScreen
import io.github.amibhai.liftlog.feature.settings.importer.ImportRoute
import io.github.amibhai.liftlog.feature.settings.plates.PlatesRoute
import io.github.amibhai.liftlog.feature.settings.warmup.WarmupRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

@Serializable
data object PlatesDestination

@Serializable
data object WarmupDestination

@Serializable
data object DataDestination

/** [uri] is the document to import. */
@Serializable
data class ImportDestination(val uri: String)

@Serializable
data object AboutDestination

@Serializable
data object ComponentGalleryDestination

/**
 * Settings and their pages, About & attributions, and (debug builds only, [showDeveloperOptions]) the
 * component gallery for trying the logging controls on a phone.
 */
fun NavGraphBuilder.settingsGraph(navController: NavController, appVersion: String, showDeveloperOptions: Boolean) {
    val back: () -> Unit = { navController.popBackStack() }
    val navigation = SettingsNavigation(
        onBack = back,
        onOpenPlates = { navController.navigate(PlatesDestination) },
        onOpenWarmup = { navController.navigate(WarmupDestination) },
        onOpenData = { navController.navigate(DataDestination) },
        onOpenAbout = { navController.navigate(AboutDestination) },
        onOpenGallery = { navController.navigate(ComponentGalleryDestination) },
    )
    composable<SettingsDestination> { SettingsRoute(showDeveloperOptions, navigation) }
    composable<PlatesDestination> { PlatesRoute(onBack = back) }
    composable<WarmupDestination> { WarmupRoute(onBack = back) }
    composable<DataDestination> {
        DataRoute(onBack = back, onImport = { navController.navigate(ImportDestination(it)) })
    }
    composable<ImportDestination> { ImportRoute(onBack = back) }
    composable<AboutDestination> { AboutRoute(appVersion = appVersion, onBack = back) }
    composable<ComponentGalleryDestination> { ComponentGalleryScreen(onBack = back) }
}
