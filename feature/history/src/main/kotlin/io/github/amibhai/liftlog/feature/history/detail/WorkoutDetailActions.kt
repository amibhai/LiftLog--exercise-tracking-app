package io.github.amibhai.liftlog.feature.history.detail

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId

/** What the workout detail screen can do. */
@Stable
class WorkoutDetailActions(
    val onBack: () -> Unit,
    /** Opens the workout screen: to edit this workout, or to go on with the one in progress. */
    val onOpenWorkout: (WorkoutId) -> Unit,
    val onOpenExercise: (ExerciseId) -> Unit,
    val onRepeat: () -> Unit,
    val onSaveAsRoutine: (name: String) -> Unit,
    val onUpdateRoutine: () -> Unit,
    val onDelete: () -> Unit,
)
