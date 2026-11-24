package io.github.amibhai.liftlog.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.feature.history.HistoryScreen
import kotlinx.serialization.Serializable

@Serializable
data object HistoryDestination

fun NavGraphBuilder.historyScreen() {
    composable<HistoryDestination> { HistoryScreen() }
}
