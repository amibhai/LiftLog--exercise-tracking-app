package io.github.amibhai.liftlog.feature.workout.logging

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId

/** Where the workout screen leads. */
@Stable
class WorkoutNavigation(
    val onClose: () -> Unit,
    val onAddExercises: (WorkoutId) -> Unit,
    val onReplaceExercise: (WorkoutId, WorkoutExerciseId) -> Unit,
    val onOpenExercise: (ExerciseId) -> Unit,
)
