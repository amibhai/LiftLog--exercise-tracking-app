package io.github.amibhai.liftlog.core.domain.model

/** A planned exercise (SPEC §7). Rep range and effort are optional targets. */
data class RoutineExercise(
    val id: RoutineExerciseId,
    val exercise: ExerciseInfo,
    val supersetGroup: Int? = null,
    val targetSets: Int = DEFAULT_SETS,
    val repMin: Int? = null,
    val repMax: Int? = null,
    val targetRpe: Double? = null,
    val targetRir: Int? = null,
    val restSeconds: Int? = null,
    val progressionRule: ProgressionRuleType = ProgressionRuleType.DOUBLE_PROGRESSION,
    val notes: String? = null,
) {
    companion object {
        const val DEFAULT_SETS = 3
    }
}

data class Routine(
    val id: RoutineId,
    val name: String,
    val notes: String? = null,
    val exercises: List<RoutineExercise> = emptyList(),
)

/** A routine in a list, with when it was last done. */
data class RoutineSummary(val routine: Routine, val lastPerformedAt: Long?)
