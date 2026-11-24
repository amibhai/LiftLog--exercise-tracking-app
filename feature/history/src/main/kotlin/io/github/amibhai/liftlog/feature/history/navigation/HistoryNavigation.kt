package io.github.amibhai.liftlog.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.feature.history.HistoryRoute
import io.github.amibhai.liftlog.feature.history.detail.WorkoutDetailRoute
import kotlinx.serialization.Serializable

@Serializable
data object HistoryDestination

/** A finished workout; [justFinished] when it was finished a moment ago on the workout screen. */
@Serializable
data class WorkoutDetailDestination(val workoutId: String, val justFinished: Boolean = false)

/**
 * The history tab and workout details. [onOpenWorkout] opens the workout screen, to edit a finished
 * workout or to go on with the one in progress.
 */
fun NavGraphBuilder.historyGraph(
    navController: NavController,
    onOpenWorkout: (WorkoutId) -> Unit,
    onOpenExercise: (ExerciseId) -> Unit,
) {
    composable<HistoryDestination> {
        HistoryRoute(onOpenWorkout = { navController.navigateToWorkoutDetail(it) })
    }
    composable<WorkoutDetailDestination> {
        WorkoutDetailRoute(
            onBack = navController::popBackStack,
            onOpenWorkout = onOpenWorkout,
            onOpenExercise = onOpenExercise,
        )
    }
}

fun NavController.navigateToWorkoutDetail(
    id: WorkoutId,
    justFinished: Boolean = false,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(WorkoutDetailDestination(id.value, justFinished), builder)
}
