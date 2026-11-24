package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId

/* Pure edits of a workout's exercises. See SetEdits for how they are stored. */

fun Workout.editExercise(id: WorkoutExerciseId, transform: (WorkoutExercise) -> WorkoutExercise): Workout =
    copy(exercises = exercises.map { if (it.id == id) transform(it) else it })

fun Workout.addExercises(added: List<WorkoutExercise>, asSuperset: Boolean): Workout =
    copy(exercises = workoutSupersets.add(exercises, added, asSuperset))

/** Keeps the sets (SPEC §5); numbers the new exercise type does not log are dropped. */
fun Workout.replaceExercise(id: WorkoutExerciseId, replacement: ExerciseInfo): Workout = editExercise(id) { exercise ->
    val type = replacement.type
    exercise.copy(
        exercise = replacement,
        routineExerciseId = null,
        sets = exercise.sets.map { it.copy(entered = it.entered.forType(type), target = it.target.forType(type)) },
    )
}

fun Workout.removeExercise(id: WorkoutExerciseId): Workout =
    copy(exercises = workoutSupersets.removeAt(exercises, indexOf(id)))

/** Undoes [removeExercise]; [original] is the workout before it, for the supersets it had. */
fun Workout.insertExercise(index: Int, exercise: WorkoutExercise, original: Workout): Workout {
    val groups = original.exercises.associate { it.id to it.supersetGroup }
    return copy(exercises = workoutSupersets.restore(exercises, index, exercise) { groups[it.id] })
}

fun Workout.moveExercise(from: Int, to: Int): Workout = copy(exercises = workoutSupersets.move(exercises, from, to))

/** The whole new order at once; exercises missing from [order] go to the end. */
fun Workout.reorderExercises(order: List<WorkoutExerciseId>): Workout =
    copy(exercises = workoutSupersets.reorder(exercises, order) { it.id })

/** Groups [others] with [anchor] (joining its superset, if any) and moves them next to it. */
fun Workout.supersetWith(anchor: WorkoutExerciseId, others: Set<WorkoutExerciseId>): Workout =
    copy(exercises = workoutSupersets.join(exercises, indexOf(anchor), others.map(::indexOf).toSet()))

fun Workout.leaveSuperset(id: WorkoutExerciseId): Workout =
    copy(exercises = workoutSupersets.leave(exercises, indexOf(id)))

private fun Workout.indexOf(id: WorkoutExerciseId) = exercises.indexOfFirst { it.id == id }
