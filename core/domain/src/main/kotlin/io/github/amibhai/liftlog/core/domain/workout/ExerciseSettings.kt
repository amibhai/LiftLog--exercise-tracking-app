package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit

/** What −/+ add, in the active unit: the exercise's own weight step, or the default one (SPEC §2). */
fun ExerciseInfo.weightStep(settings: UserSettings): Double {
    val own = when (settings.weightUnit) {
        WeightUnit.KG -> defaults.weightStepKg
        WeightUnit.LB -> defaults.weightStepLb
    }
    return own ?: settings.weightStep(equipment, settings.weightUnit)
}

/** The empty bar; null for exercises that are not loaded with plates. */
fun ExerciseInfo.barWeight(settings: UserSettings): Weight? =
    defaults.barWeight ?: settings.barWeight(equipment, settings.weightUnit)
