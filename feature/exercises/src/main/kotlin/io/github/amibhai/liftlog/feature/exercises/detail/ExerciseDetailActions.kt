package io.github.amibhai.liftlog.feature.exercises.detail

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseId

@Stable
internal class ExerciseDetailActions(
    val onBack: () -> Unit,
    val onEdit: (ExerciseId) -> Unit,
    val onFavoriteChange: (Boolean) -> Unit,
    val onHiddenChange: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onNoteChange: (String) -> Unit,
    val onDefaultsChange: (ExerciseDefaults) -> Unit,
)
