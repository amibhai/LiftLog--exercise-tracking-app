package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.units.Weight

/** What the warm-up leads to: the weight of the first working set. */
val WorkoutExercise.workingWeight: Weight?
    get() = sets.firstOrNull { it.type.isMain }?.shown?.weight?.takeIf { it > Weight.ZERO }

/** Puts [warmups] before the working sets. Warm-ups that were not done yet make room for them. */
fun Workout.withWarmups(id: WorkoutExerciseId, warmups: List<WorkoutSet>): Workout = editExercise(id) { exercise ->
    val kept = exercise.sets.filterNot { it.type == SetType.WARMUP && !it.isCompleted }
    val done = kept.takeWhile { it.type == SetType.WARMUP }
    exercise.copy(sets = done + warmups + kept.drop(done.size))
}
