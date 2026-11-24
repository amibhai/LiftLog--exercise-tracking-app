package io.github.amibhai.liftlog.feature.progress.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.feature.progress.ProgressActions
import io.github.amibhai.liftlog.feature.progress.ProgressRoute
import io.github.amibhai.liftlog.feature.progress.body.BodyRoute
import io.github.amibhai.liftlog.feature.progress.body.MetricRoute
import kotlinx.serialization.Serializable

@Serializable
data object ProgressDestination

/** Body weight, measurements and photos. */
@Serializable
data object BodyDestination

/** [type] is the name of a kind of measurement. */
@Serializable
data class MetricDestination(val type: String)

/** The progress tab and what it opens. */
fun NavGraphBuilder.progressGraph(navController: NavController, onOpenExercise: (ExerciseId) -> Unit) {
    val actions = ProgressActions(
        onOpenExercise = onOpenExercise,
        onOpenBody = { navController.navigate(BodyDestination) },
    )
    composable<ProgressDestination> { ProgressRoute(actions) }
    composable<BodyDestination> {
        BodyRoute(
            onBack = navController::popBackStack,
            onOpenMetric = { navController.navigate(MetricDestination(it.name)) },
        )
    }
    composable<MetricDestination> { MetricRoute(onBack = navController::popBackStack) }
}
