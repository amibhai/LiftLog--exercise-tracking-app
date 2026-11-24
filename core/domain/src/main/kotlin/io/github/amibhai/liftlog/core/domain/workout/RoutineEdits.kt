package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId

/* Pure edits of a routine, stored like workout edits. */

fun Routine.editExercise(id: RoutineExerciseId, transform: (RoutineExercise) -> RoutineExercise): Routine =
    copy(exercises = exercises.map { if (it.id == id) transform(it) else it })

fun Routine.addExercises(added: List<RoutineExercise>, asSuperset: Boolean): Routine =
    copy(exercises = routineSupersets.add(exercises, added, asSuperset))

fun Routine.removeExercise(id: RoutineExerciseId): Routine =
    copy(exercises = routineSupersets.removeAt(exercises, indexOf(id)))

/** Undoes [removeExercise]; [original] is the routine before it, for the supersets it had. */
fun Routine.insertExercise(index: Int, exercise: RoutineExercise, original: Routine): Routine {
    val groups = original.exercises.associate { it.id to it.supersetGroup }
    return copy(exercises = routineSupersets.restore(exercises, index, exercise) { groups[it.id] })
}

fun Routine.moveExercise(from: Int, to: Int): Routine = copy(exercises = routineSupersets.move(exercises, from, to))

/** The whole new order at once; exercises missing from [order] go to the end. */
fun Routine.reorderExercises(order: List<RoutineExerciseId>): Routine =
    copy(exercises = routineSupersets.reorder(exercises, order) { it.id })

fun Routine.supersetWith(anchor: RoutineExerciseId, others: Set<RoutineExerciseId>): Routine =
    copy(exercises = routineSupersets.join(exercises, indexOf(anchor), others.map(::indexOf).toSet()))

fun Routine.leaveSuperset(id: RoutineExerciseId): Routine =
    copy(exercises = routineSupersets.leave(exercises, indexOf(id)))

private fun Routine.indexOf(id: RoutineExerciseId) = exercises.indexOfFirst { it.id == id }
