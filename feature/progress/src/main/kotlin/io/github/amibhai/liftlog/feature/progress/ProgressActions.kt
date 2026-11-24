package io.github.amibhai.liftlog.feature.progress

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId

/** Where the dashboard leads. */
@Stable
class ProgressActions(val onOpenExercise: (ExerciseId) -> Unit, val onOpenBody: () -> Unit)
