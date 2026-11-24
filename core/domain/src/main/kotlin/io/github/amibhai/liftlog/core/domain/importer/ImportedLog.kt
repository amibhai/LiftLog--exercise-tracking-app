package io.github.amibhai.liftlog.core.domain.importer

import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.units.DistanceUnit
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.time.LocalDateTime

/** The apps whose exports can be read (SPEC §9). */
enum class LogSource { STRONG, HEVY }

/** A training log of another app, as its export has it. */
data class ImportedLog(
    val source: LogSource,
    /** The unit of the weights; null when the export does not say. */
    val unit: WeightUnit?,
    val workouts: List<ImportedWorkout>,
    /** Rows that could not be made sense of. */
    val unreadableRows: Int,
) {
    /** The names of the exercises, the most frequent first. */
    val exerciseNames: List<String>
        get() = workouts.flatMap { it.exercises }
            .groupingBy { it.name }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
}

/** Times are local to wherever the workout took place. */
data class ImportedWorkout(
    val name: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val notes: String?,
    val exercises: List<ImportedExercise>,
)

data class ImportedExercise(
    val name: String,
    /** Neighbours with the same value form a superset. */
    val superset: String?,
    val notes: String?,
    val sets: List<ImportedSet>,
)

/** Numbers as they are in the file; null when there is none or it is zero. */
data class ImportedSet(
    val type: SetType = SetType.WORKING,
    val weight: Double? = null,
    val reps: Int? = null,
    val distance: Double? = null,
    /** Null when the file does not say. */
    val distanceUnit: DistanceUnit? = null,
    val seconds: Int? = null,
    val rpe: Double? = null,
)
