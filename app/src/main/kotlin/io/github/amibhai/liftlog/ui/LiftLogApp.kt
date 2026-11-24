package io.github.amibhai.liftlog.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.amibhai.liftlog.feature.exercises.navigation.exercisesGraph
import io.github.amibhai.liftlog.feature.exercises.navigation.navigateToExerciseDetail
import io.github.amibhai.liftlog.feature.exercises.navigation.navigateToExercisePicker
import io.github.amibhai.liftlog.feature.history.navigation.historyGraph
import io.github.amibhai.liftlog.feature.history.navigation.navigateToWorkoutDetail
import io.github.amibhai.liftlog.feature.home.navigation.HomeDestination
import io.github.amibhai.liftlog.feature.home.navigation.homeScreen
import io.github.amibhai.liftlog.feature.progress.navigation.progressGraph
import io.github.amibhai.liftlog.feature.routines.editor.RoutineEditorNavigation
import io.github.amibhai.liftlog.feature.routines.navigation.navigateToRoutineEditor
import io.github.amibhai.liftlog.feature.routines.navigation.routineEditorScreen
import io.github.amibhai.liftlog.feature.settings.navigation.AboutDestination
import io.github.amibhai.liftlog.feature.settings.navigation.ComponentGalleryDestination
import io.github.amibhai.liftlog.feature.settings.navigation.SettingsDestination
import io.github.amibhai.liftlog.feature.settings.navigation.settingsGraph
import io.github.amibhai.liftlog.feature.workout.active.ActiveWorkoutBanner
import io.github.amibhai.liftlog.feature.workout.logging.WorkoutNavigation
import io.github.amibhai.liftlog.feature.workout.navigation.WorkoutDestination
import io.github.amibhai.liftlog.feature.workout.navigation.navigateToWorkout
import io.github.amibhai.liftlog.feature.workout.navigation.workoutScreen

private val MinTabLabelSize = 8.sp

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
    val workoutNavigation = remember(navController) {
        WorkoutNavigation(
            onClose = navController::popBackStack,
            onAddExercises = { navController.navigateToExercisePicker(it) },
            onReplaceExercise = { workout, slot -> navController.navigateToExercisePicker(workout, slot) },
            onOpenExercise = { navController.navigateToExerciseDetail(it) },
        )
    }
    val routineNavigation = remember(navController) {
        RoutineEditorNavigation(
            onBack = navController::popBackStack,
            onAddExercises = { navController.navigateToExercisePicker(it) },
            onReplaceExercise = { routine, slot -> navController.navigateToExercisePicker(routine, slot) },
            onOpenExercise = { navController.navigateToExerciseDetail(it) },
        )
    }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) {
                Column {
                    // SPEC §5: a workout in progress is one tap away from every tab.
                    ActiveWorkoutBanner(onOpen = { navController.navigateToWorkout(it) })
                    NavigationBar {
                        TopLevelDestination.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == currentTab,
                                onClick = { navController.navigateToTab(tab) },
                                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                                label = { TabLabel(stringResource(tab.label)) },
                            )
                        }
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
                onOpenWorkout = { navController.navigateToWorkout(it) },
                onEditRoutine = { navController.navigateToRoutineEditor(it) },
            )
            routineEditorScreen(routineNavigation)
            historyGraph(
                navController = navController,
                onOpenWorkout = { navController.navigateToWorkout(it) },
                onOpenExercise = { navController.navigateToExerciseDetail(it) },
            )
            workoutScreen(
                navigation = workoutNavigation,
                onFinished = { id ->
                    navController.navigateToWorkoutDetail(id, justFinished = true) {
                        popUpTo<WorkoutDestination> { inclusive = true }
                    }
                },
            )
            progressGraph(
                navController = navController,
                onOpenExercise = { navController.navigateToExerciseDetail(it) },
            )
            exercisesGraph(navController)
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

/** Shrinks at large font sizes instead of breaking "Exercises" mid-word. */
@Composable
private fun TabLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MinTabLabelSize,
            maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
        ),
        softWrap = false,
        maxLines = 1,
    )
}
