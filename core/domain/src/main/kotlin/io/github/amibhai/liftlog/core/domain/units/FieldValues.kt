package io.github.amibhai.liftlog.core.domain.units

import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetValues
import java.math.BigDecimal

/** One value as the user reads and types it: in [unit], reps as reps, times in seconds. */
fun SetValues.shown(field: SetField, type: ExerciseType, unit: WeightUnit): BigDecimal? = when (field) {
    SetField.WEIGHT -> weight?.displayValue(unit)
    SetField.REPS -> reps?.toBigDecimal()
    SetField.DURATION -> durationSeconds?.toBigDecimal()
    SetField.DISTANCE -> distanceMeters?.let { DistanceUnit.of(type, unit).display(it) }
}

/** These values with one of them replaced by what was typed; null clears it. */
fun SetValues.enter(field: SetField, value: BigDecimal?, type: ExerciseType, unit: WeightUnit): SetValues =
    when (field) {
        SetField.WEIGHT -> copy(weight = value?.let { Weight.of(it, unit) })
        SetField.REPS -> copy(reps = value?.toInt())
        SetField.DURATION -> copy(durationSeconds = value?.toInt())
        SetField.DISTANCE -> copy(distanceMeters = value?.let { DistanceUnit.of(type, unit).toMeters(it) })
    }
