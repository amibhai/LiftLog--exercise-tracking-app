package io.github.amibhai.liftlog.core.domain.model

/** What the exercise editor saves (SPEC §3). A draft without [id] becomes a new custom exercise. */
data class ExerciseDraft(
    val id: ExerciseId? = null,
    val name: String = "",
    val type: ExerciseType = ExerciseType.WEIGHT_REPS,
    val equipment: Equipment = Equipment.BARBELL,
    val category: ExerciseCategory = ExerciseCategory.STRENGTH,
    val primaryMuscles: List<Muscle> = emptyList(),
    val secondaryMuscles: List<Muscle> = emptyList(),
    val isUnilateral: Boolean = false,
    val instructions: List<String> = emptyList(),
) {
    val isValid: Boolean get() = name.isNotBlank() && primaryMuscles.isNotEmpty()
}
