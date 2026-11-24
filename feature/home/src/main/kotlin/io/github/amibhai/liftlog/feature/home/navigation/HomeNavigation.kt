package io.github.amibhai.liftlog.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

/** [onOpenWorkout] opens the workout screen; [onEditRoutine] the routine editor. */
fun NavGraphBuilder.homeScreen(
    onOpenSettings: () -> Unit,
    onBrowseExercises: () -> Unit,
    onOpenWorkout: (WorkoutId) -> Unit,
    onEditRoutine: (RoutineId) -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(
            onOpenSettings = onOpenSettings,
            onBrowseExercises = onBrowseExercises,
            onOpenWorkout = onOpenWorkout,
            onEditRoutine = onEditRoutine,
        )
    }
}
