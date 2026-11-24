package io.github.amibhai.liftlog.feature.workout

import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal const val NOW = 1_700_000_000_000L
internal const val TEN_MINUTES = 600_000L

internal val testClock: Clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)

internal val bench = ExerciseInfo(ExerciseId("bench"), "Bench Press", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
internal val row = ExerciseInfo(ExerciseId("row"), "Barbell Row", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)

internal fun values(kg: Double? = null, reps: Int? = null) = SetValues(weight = kg?.let(::Weight), reps = reps)

/** A set planned at [target]; [entered] is what was typed. */
internal fun set(
    id: String,
    target: SetValues = SetValues.EMPTY,
    entered: SetValues = SetValues.EMPTY,
    type: SetType = SetType.WORKING,
    done: Boolean = false,
) = WorkoutSet(
    id = WorkoutSetId(id),
    type = type,
    entered = if (done && entered.isEmpty) target else entered,
    target = target,
    isCompleted = done,
    completedAt = if (done) NOW else null,
)

internal fun exercise(id: String, info: ExerciseInfo, vararg sets: WorkoutSet, group: Int? = null) =
    WorkoutExercise(WorkoutExerciseId(id), info, supersetGroup = group, sets = sets.toList())

internal fun workout(vararg exercises: WorkoutExercise, inProgress: Boolean = true) = Workout(
    id = WorkoutId("w"),
    startedAt = NOW - TEN_MINUTES,
    endedAt = if (inProgress) null else NOW,
    isInProgress = inProgress,
    exercises = exercises.toList(),
)
