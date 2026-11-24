package io.github.amibhai.liftlog.feature.home

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId

/** What the home screen can do. */
@Stable
class HomeActions(
    val onOpenSettings: () -> Unit,
    val onBrowseExercises: () -> Unit,
    /** Goes on with the workout in progress. */
    val onResumeWorkout: (WorkoutId) -> Unit,
    val onStartEmpty: () -> Unit,
    val onRepeatLast: () -> Unit,
    val onStartRoutine: (RoutineId) -> Unit,
    val onNewRoutine: () -> Unit,
    val onEditRoutine: (RoutineId) -> Unit,
    val onDeleteRoutine: (RoutineId) -> Unit,
    val onUndoDeleteRoutine: () -> Unit,
    val onDeleteMessageShown: () -> Unit,
)
