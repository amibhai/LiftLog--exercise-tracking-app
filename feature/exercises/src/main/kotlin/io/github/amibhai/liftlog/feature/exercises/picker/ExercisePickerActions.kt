package io.github.amibhai.liftlog.feature.exercises.picker

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.feature.exercises.ExerciseListActions

@Stable
internal class ExercisePickerActions(
    val list: ExerciseListActions,
    val onPick: (ExerciseId) -> Unit,
    val onAdd: (asSuperset: Boolean) -> Unit,
    val onClose: () -> Unit,
)
