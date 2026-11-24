package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet

/** Where a set is. Enough to put it back after removing it. */
data class LocatedSet(val exerciseId: WorkoutExerciseId, val index: Int, val set: WorkoutSet)
