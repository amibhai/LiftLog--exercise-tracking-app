package io.github.amibhai.liftlog.feature.exercises.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.feature.exercises.ExercisesRoute
import io.github.amibhai.liftlog.feature.exercises.detail.ExerciseDetailRoute
import kotlinx.serialization.Serializable

@Serializable
data object ExercisesDestination

@Serializable
data class ExerciseDetailDestination(val exerciseId: String)

fun NavGraphBuilder.exercisesScreen(onOpenExercise: (exerciseId: String) -> Unit) {
    composable<ExercisesDestination> { ExercisesRoute(onOpenExercise = onOpenExercise) }
}

fun NavGraphBuilder.exerciseDetailScreen(onBack: () -> Unit) {
    composable<ExerciseDetailDestination> { ExerciseDetailRoute(onBack = onBack) }
}

fun NavController.navigateToExerciseDetail(exerciseId: String) {
    navigate(ExerciseDetailDestination(exerciseId))
}
