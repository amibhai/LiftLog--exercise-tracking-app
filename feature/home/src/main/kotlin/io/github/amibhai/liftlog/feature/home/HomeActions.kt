package io.github.amibhai.liftlog.feature.home

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId

/** Where the home screen leads; the app module decides. */
@Stable
class HomeNavigation(
    val onOpenSettings: () -> Unit,
    val onBrowseExercises: () -> Unit,
    val onOpenProgress: () -> Unit,
    val onOpenExercise: (ExerciseId) -> Unit,
    val onOpenPrograms: () -> Unit,
    val onOpenWorkout: (WorkoutId) -> Unit,
    val onEditRoutine: (RoutineId) -> Unit,
)

/** What the home screen can do besides going somewhere. */
@Stable
class HomeActions(
    val onStartEmpty: () -> Unit,
    val onRepeatLast: () -> Unit,
    val onStartRoutine: (RoutineId) -> Unit,
    val onNewRoutine: () -> Unit,
    val onDeleteRoutine: (RoutineId) -> Unit,
    val onUndoDeleteRoutine: () -> Unit,
    val onDeleteMessageShown: () -> Unit,
)
