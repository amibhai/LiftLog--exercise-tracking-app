package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId

/*
 * Pure edits of a workout's sets. The repository applies them to the stored workout in one transaction
 * and writes only the rows that changed (SPEC §5: persisted on every change).
 */

fun Workout.findSet(id: WorkoutSetId): LocatedSet? = exercises.firstNotNullOfOrNull { exercise ->
    val index = exercise.sets.indexOfFirst { it.id == id }
    if (index < 0) null else LocatedSet(exercise.id, index, exercise.sets[index])
}

fun Workout.updateSet(id: WorkoutSetId, transform: (WorkoutSet) -> WorkoutSet): Workout = editSetsOf(id) { sets ->
    sets.map { if (it.id == id) transform(it) else it }
}

/** Logs what the row shows, and passes the new numbers on to the sets that follow. */
fun Workout.completeSet(id: WorkoutSetId, now: Long): Workout = editExerciseOf(id) { exercise ->
    val index = exercise.sets.indexOfFirst { it.id == id }
    val before = exercise.sets[index]
    val done = before.copy(
        entered = before.shown.forType(exercise.exercise.type),
        isCompleted = true,
        completedAt = now,
    )
    val sets = exercise.sets.mapIndexed { i, set ->
        when {
            i == index -> done
            i > index && set.expects(before) -> set.copy(target = set.target.following(before.target, done.entered))
            else -> set
        }
    }
    exercise.copy(sets = sets)
}

fun Workout.reopenSet(id: WorkoutSetId): Workout = updateSet(id) { it.copy(isCompleted = false, completedAt = null) }

/** "+ Add set" copies the last set; after a warm-up or a failed set that means a working set. */
fun Workout.addSet(exerciseId: WorkoutExerciseId, newId: WorkoutSetId): Workout = editExercise(exerciseId) { exercise ->
    val last = exercise.sets.lastOrNull()
    val type = last?.type.takeIf { it == SetType.DROP } ?: SetType.WORKING
    exercise.copy(sets = exercise.sets + WorkoutSet(newId, type, target = last?.shown ?: SetValues.EMPTY))
}

/** Swipe right: an open copy right below, filled with the same numbers. */
fun Workout.duplicateSet(id: WorkoutSetId, newId: WorkoutSetId): Workout = editSetsOf(id) { sets ->
    val index = sets.indexOfFirst { it.id == id }
    val copy = WorkoutSet(newId, sets[index].type, target = sets[index].shown)
    sets.take(index + 1) + copy + sets.drop(index + 1)
}

fun Workout.removeSet(id: WorkoutSetId): Workout = editSetsOf(id) { sets -> sets.filterNot { it.id == id } }

/** Undoes [removeSet] with what [findSet] returned before it. */
fun Workout.insertSet(exerciseId: WorkoutExerciseId, index: Int, set: WorkoutSet): Workout =
    editExercise(exerciseId) { exercise ->
        val at = index.coerceIn(0, exercise.sets.size)
        exercise.copy(sets = exercise.sets.take(at) + set + exercise.sets.drop(at))
    }

private fun Workout.editSetsOf(id: WorkoutSetId, transform: (List<WorkoutSet>) -> List<WorkoutSet>): Workout =
    editExerciseOf(id) { it.copy(sets = transform(it.sets)) }

private fun Workout.editExerciseOf(id: WorkoutSetId, transform: (WorkoutExercise) -> WorkoutExercise): Workout =
    copy(exercises = exercises.map { if (it.sets.any { set -> set.id == id }) transform(it) else it })

/** Whether this open set was planned together with [other], so that it follows changes to it. */
private fun WorkoutSet.expects(other: WorkoutSet): Boolean =
    !isCompleted && entered.isEmpty && (type == SetType.WARMUP) == (other.type == SetType.WARMUP)

/**
 * Goals after a set logged as [logged] that was planned as [planned]: a changed weight carries over where
 * the old one was expected, and empty goals are filled. Reps never carry over: a missed rep is not a plan.
 */
private fun SetValues.following(planned: SetValues, logged: SetValues): SetValues =
    copy(weight = if (weight == planned.weight) logged.weight ?: weight else weight).orElse(logged)
