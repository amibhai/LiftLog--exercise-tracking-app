package io.github.amibhai.liftlog.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.amibhai.liftlog.feature.exercises.navigation.exerciseDetailScreen
import io.github.amibhai.liftlog.feature.exercises.navigation.exercisesScreen
import io.github.amibhai.liftlog.feature.exercises.navigation.navigateToExerciseDetail
import io.github.amibhai.liftlog.feature.history.navigation.historyScreen
import io.github.amibhai.liftlog.feature.home.navigation.HomeDestination
import io.github.amibhai.liftlog.feature.home.navigation.homeScreen
import io.github.amibhai.liftlog.feature.progress.navigation.progressScreen
import io.github.amibhai.liftlog.feature.settings.navigation.AboutDestination
import io.github.amibhai.liftlog.feature.settings.navigation.ComponentGalleryDestination
import io.github.amibhai.liftlog.feature.settings.navigation.SettingsDestination
import io.github.amibhai.liftlog.feature.settings.navigation.settingsGraph

/**
 * The navigation shell: a bottom bar for the top-level tabs (hidden on detail screens) around a
 * type-safe NavHost. Each screen draws its own top bar and handles its own insets.
 */
@Composable
fun LiftLogApp(appVersion: String, showDeveloperOptions: Boolean, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            homeScreen(
                onOpenSettings = { navController.navigate(SettingsDestination) },
                onBrowseExercises = { navController.navigateToTab(TopLevelDestination.EXERCISES) },
            )
            historyScreen()
            progressScreen()
            exercisesScreen(onOpenExercise = { navController.navigateToExerciseDetail(it) })
            exerciseDetailScreen(onBack = { navController.popBackStack() })
            settingsGraph(
                appVersion = appVersion,
                showDeveloperOptions = showDeveloperOptions,
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(AboutDestination) },
                onOpenGallery = { navController.navigate(ComponentGalleryDestination) },
            )
        }
    }
}

/** Switches tabs keeping each tab's back stack (SPEC §13: predictive back works per tab). */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
