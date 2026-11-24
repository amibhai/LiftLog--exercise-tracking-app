package io.github.amibhai.liftlog.feature.exercises

import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Muscle

/** What the list screens can ask for. */
@Stable
internal class ExerciseListActions(
    val onMuscleChange: (Muscle?) -> Unit,
    val onEquipmentChange: (Equipment?) -> Unit,
    val onOpenExercise: (ExerciseId) -> Unit,
    val onCreateExercise: (name: String) -> Unit,
)
