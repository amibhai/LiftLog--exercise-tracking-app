package io.github.amibhai.liftlog.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(onOpenSettings: () -> Unit, onBrowseExercises: () -> Unit) {
    composable<HomeDestination> {
        HomeRoute(onOpenSettings = onOpenSettings, onBrowseExercises = onBrowseExercises)
    }
}
