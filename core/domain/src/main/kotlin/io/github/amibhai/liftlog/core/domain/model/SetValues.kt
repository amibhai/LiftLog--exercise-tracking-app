package io.github.amibhai.liftlog.core.domain.model

import io.github.amibhai.liftlog.core.domain.units.Weight

/** The numbers of one set. Which of them matter depends on the [ExerciseType]. */
data class SetValues(
    val weight: Weight? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
) {
    val isEmpty: Boolean get() = this == EMPTY

    fun has(field: SetField): Boolean = when (field) {
        SetField.WEIGHT -> weight != null
        SetField.REPS -> reps != null
        SetField.DURATION -> durationSeconds != null
        SetField.DISTANCE -> distanceMeters != null
    }

    /** These values, with gaps filled from [fallback]. */
    fun orElse(fallback: SetValues) = SetValues(
        weight = weight ?: fallback.weight,
        reps = reps ?: fallback.reps,
        durationSeconds = durationSeconds ?: fallback.durationSeconds,
        distanceMeters = distanceMeters ?: fallback.distanceMeters,
    )

    /** Only the values [type] logs, so switching an exercise never leaves stray numbers behind. */
    fun forType(type: ExerciseType) = SetValues(
        weight = weight.takeIf { SetField.WEIGHT in type.fields },
        reps = reps.takeIf { SetField.REPS in type.fields },
        durationSeconds = durationSeconds.takeIf { SetField.DURATION in type.fields },
        distanceMeters = distanceMeters.takeIf { SetField.DISTANCE in type.fields },
    )

    companion object {
        val EMPTY = SetValues()
    }
}

/**
 * The first field that must be filled before a set can be completed; null when it can. Weight is
 * optional on bodyweight exercises and carries, and cardio needs either distance or time.
 */
fun ExerciseType.missingField(values: SetValues): SetField? = when (this) {
    ExerciseType.WEIGHT_REPS -> listOf(SetField.WEIGHT, SetField.REPS).firstOrNull { !values.has(it) }

    ExerciseType.BODYWEIGHT_ADDED, ExerciseType.BODYWEIGHT_ASSISTED, ExerciseType.REPS_ONLY ->
        SetField.REPS.takeIf { !values.has(it) }

    ExerciseType.DURATION -> SetField.DURATION.takeIf { !values.has(it) }

    ExerciseType.DISTANCE_DURATION -> SetField.DISTANCE.takeIf { fields.none(values::has) }

    ExerciseType.WEIGHT_DISTANCE -> SetField.DISTANCE.takeIf { !values.has(it) }
}
