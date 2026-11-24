package io.github.amibhai.liftlog.feature.exercises

import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSummary
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle

internal fun summary(name: String) = ExerciseSummary(
    id = ExerciseId(name),
    name = name,
    type = ExerciseType.WEIGHT_REPS,
    equipment = Equipment.BARBELL,
    primaryMuscle = Muscle.CHEST,
    thumbnailUrl = null,
    isCustom = false,
)
