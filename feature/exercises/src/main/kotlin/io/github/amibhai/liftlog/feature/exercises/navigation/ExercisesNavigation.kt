package io.github.amibhai.liftlog.feature.exercises.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.feature.exercises.ExercisesRoute
import io.github.amibhai.liftlog.feature.exercises.detail.ExerciseDetailRoute
import io.github.amibhai.liftlog.feature.exercises.editor.ExerciseEditorRoute
import io.github.amibhai.liftlog.feature.exercises.picker.ExercisePickerRoute
import kotlinx.serialization.Serializable

@Serializable
data object ExercisesDestination

@Serializable
data class ExerciseDetailDestination(val exerciseId: String)

/** Edits [exerciseId], or creates a custom exercise called [name]. */
@Serializable
data class ExerciseEditorDestination(val exerciseId: String? = null, val name: String? = null)

/** Adds exercises to a workout or a routine; with a [slotId] it replaces that exercise instead. */
@Serializable
data class ExercisePickerDestination(
    val workoutId: String? = null,
    val routineId: String? = null,
    val slotId: String? = null,
)

/** Where the editor leaves the id of a new exercise for the screen it returns to. */
private const val CREATED_EXERCISE = "created_exercise"

fun NavGraphBuilder.exercisesGraph(navController: NavController) {
    val openDetail: (ExerciseId) -> Unit = { navController.navigateToExerciseDetail(it) }
    val create: (String) -> Unit = { navController.navigate(ExerciseEditorDestination(name = it)) }

    composable<ExercisesDestination> { ExercisesRoute(onOpenExercise = openDetail, onCreateExercise = create) }

    composable<ExerciseDetailDestination> {
        ExerciseDetailRoute(
            onBack = navController::popBackStack,
            onEdit = { navController.navigate(ExerciseEditorDestination(exerciseId = it.value)) },
        )
    }

    composable<ExerciseEditorDestination> {
        ExerciseEditorRoute(
            onClose = navController::popBackStack,
            onSave = { id ->
                navController.previousBackStackEntry?.savedStateHandle?.set(CREATED_EXERCISE, id.value)
                navController.popBackStack()
            },
        )
    }

    composable<ExercisePickerDestination> { entry ->
        val created by entry.savedStateHandle.getStateFlow<String?>(CREATED_EXERCISE, null)
            .collectAsStateWithLifecycle()
        ExercisePickerRoute(
            createdExercise = created,
            onClose = navController::popBackStack,
            onOpenExercise = openDetail,
            onCreateExercise = create,
        )
    }
}

fun NavController.navigateToExerciseDetail(id: ExerciseId) {
    navigate(ExerciseDetailDestination(id.value))
}

fun NavController.navigateToExercisePicker(workout: WorkoutId, slot: WorkoutExerciseId? = null) {
    navigate(ExercisePickerDestination(workoutId = workout.value, slotId = slot?.value))
}

fun NavController.navigateToExercisePicker(routine: RoutineId, slot: RoutineExerciseId? = null) {
    navigate(ExercisePickerDestination(routineId = routine.value, slotId = slot?.value))
}
