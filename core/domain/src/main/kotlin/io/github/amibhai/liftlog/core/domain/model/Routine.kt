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
    val progression: ProgressionRule = ProgressionRule.DoubleProgression,
    val notes: String? = null,
) {
    companion object {
        const val DEFAULT_SETS = 3
        private const val DEFAULT_REP_MIN = 8
        private const val DEFAULT_REP_MAX = 12

        /** An exercise added by hand: with a rep range, so that its progression can start. */
        fun new(id: RoutineExerciseId, exercise: ExerciseInfo): RoutineExercise {
            val hasReps = SetField.REPS in exercise.type.fields
            return RoutineExercise(
                id = id,
                exercise = exercise,
                repMin = DEFAULT_REP_MIN.takeIf { hasReps },
                repMax = DEFAULT_REP_MAX.takeIf { hasReps },
            )
        }
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
