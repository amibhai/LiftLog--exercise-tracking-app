package io.github.amibhai.liftlog.feature.workout.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.feature.workout.logging.WorkoutNavigation
import io.github.amibhai.liftlog.feature.workout.logging.WorkoutRoute
import kotlinx.serialization.Serializable

/** Logs the workout in progress, or edits a finished one. */
@Serializable
data class WorkoutDestination(val workoutId: String)

/**
 * The workout screen. [onFinished] gets the finished workout; the workout screen is already off the back
 * stack by then, so that back from what opens next does not return to it.
 */
fun NavGraphBuilder.workoutScreen(navigation: WorkoutNavigation, onFinished: (WorkoutId) -> Unit) {
    composable<WorkoutDestination> {
        WorkoutRoute(navigation = navigation, onFinish = onFinished)
    }
}

/** Opens a workout, or goes back to it where it is open already. */
fun NavController.navigateToWorkout(id: WorkoutId) {
    val destination = WorkoutDestination(id.value)
    val top = currentBackStackEntry?.takeIf { it.destination.hasRoute<WorkoutDestination>() }
    // Not single top: that would put this workout into the screen of another one, such as a past one being edited.
    if (top?.toRoute<WorkoutDestination>() != destination && !popBackStack(destination, inclusive = false)) {
        navigate(destination)
    }
}
