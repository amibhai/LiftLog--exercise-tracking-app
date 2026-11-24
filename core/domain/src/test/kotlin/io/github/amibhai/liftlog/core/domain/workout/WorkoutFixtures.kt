package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight

internal const val NOW = 1_700_000_000_000L

internal fun values(kg: Double? = null, reps: Int? = null) = SetValues(weight = kg?.let(::Weight), reps = reps)

internal fun logged(kg: Double?, reps: Int?, type: SetType = SetType.WORKING) = LoggedSet(type, values(kg, reps))

internal fun info(name: String = "Bench", type: ExerciseType = ExerciseType.WEIGHT_REPS) =
    ExerciseInfo(id = ExerciseId(name.lowercase()), name = name, type = type, equipment = Equipment.BARBELL)

/** A set that is [done] has logged its shown values. */
internal fun set(
    id: String,
    type: SetType = SetType.WORKING,
    target: SetValues = SetValues.EMPTY,
    entered: SetValues = SetValues.EMPTY,
    done: Boolean = false,
) = WorkoutSet(
    id = WorkoutSetId(id),
    type = type,
    entered = if (done) entered.orElse(target) else entered,
    target = target,
    isCompleted = done,
    completedAt = if (done) NOW - 1 else null,
)

internal fun exercise(
    id: String,
    vararg sets: WorkoutSet,
    group: Int? = null,
    type: ExerciseType = ExerciseType.WEIGHT_REPS,
) = WorkoutExercise(
    id = WorkoutExerciseId(id),
    exercise = info(id, type),
    supersetGroup = group,
    sets = sets.toList(),
)

internal fun workout(vararg exercises: WorkoutExercise) =
    Workout(id = WorkoutId("w"), startedAt = NOW - 60_000, exercises = exercises.toList())

internal fun Workout.exercise(id: String) = exercises.first { it.id.value == id }

internal fun Workout.set(id: String) = exercises.flatMap { it.sets }.first { it.id.value == id }

internal fun Workout.layout() = exercises.map { it.id.value to it.supersetGroup }
