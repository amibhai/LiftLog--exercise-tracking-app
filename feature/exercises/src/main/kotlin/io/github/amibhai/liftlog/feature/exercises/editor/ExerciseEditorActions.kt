package io.github.amibhai.liftlog.feature.exercises.editor

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.Muscle

@Stable
internal class ExerciseEditorActions(
    val onChange: ((ExerciseDraft) -> ExerciseDraft) -> Unit,
    val onTogglePrimary: (Muscle) -> Unit,
    val onToggleSecondary: (Muscle) -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
)
