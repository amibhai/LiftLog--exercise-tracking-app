package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet

/** "Save as routine" (SPEC §5): the plan that this workout carried out. */
fun routineFrom(workout: Workout, name: String, ids: IdGenerator) = Routine(
    id = RoutineId(ids.newId()),
    name = name,
    exercises = workout.exercises.map { it.toRoutineExercise(ids) },
)

/** "Update routine": takes over exercises, order, supersets and set counts, and keeps the other targets. */
fun Routine.updatedFrom(workout: Workout, ids: IdGenerator): Routine {
    val slots = exercises.associateBy { it.id }
    return copy(
        exercises = workout.exercises.map { done ->
            val slot = done.routineExerciseId?.let(slots::get)
            slot?.copy(
                supersetGroup = done.supersetGroup,
                targetSets = done.plannedSets,
                restSeconds = done.restSeconds ?: slot.restSeconds,
            ) ?: done.toRoutineExercise(ids)
        },
    )
}

/** Whether it is worth asking to update the routine after this workout. */
fun Routine.differsFrom(workout: Workout): Boolean =
    exercises.map { Triple(it.exercise.id, it.targetSets, it.supersetGroup) } !=
        workout.exercises.map { Triple(it.exercise.id, it.plannedSets, it.supersetGroup) }

private fun WorkoutExercise.toRoutineExercise(ids: IdGenerator): RoutineExercise {
    val reps = mainSets.mapNotNull { it.shown.reps }.maxOrNull()
    val range = reps?.let(RepRange::around)
    return RoutineExercise(
        id = RoutineExerciseId(ids.newId()),
        exercise = exercise,
        supersetGroup = supersetGroup,
        targetSets = plannedSets,
        repMin = range?.min,
        repMax = range?.max,
        restSeconds = restSeconds,
        notes = notes,
    )
}

private val WorkoutExercise.mainSets: List<WorkoutSet> get() = sets.filter { it.type.isMain }

private val WorkoutExercise.plannedSets: Int get() = mainSets.size.coerceAtLeast(1)
