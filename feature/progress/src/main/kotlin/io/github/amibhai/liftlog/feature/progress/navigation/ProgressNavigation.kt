package io.github.amibhai.liftlog.feature.progress.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.feature.progress.ProgressScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProgressDestination

fun NavGraphBuilder.progressScreen() {
    composable<ProgressDestination> { ProgressScreen() }
}
