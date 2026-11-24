package io.github.amibhai.liftlog.feature.history

import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
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

/** 2026-11-24 18:00 UTC. */
internal val NOW = Instant.parse("2026-11-24T18:00:00Z").toEpochMilli()
internal const val HOUR = 3_600_000L

internal val testClock: Clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)

internal val bench = ExerciseInfo(ExerciseId("bench"), "Bench Press", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
internal val squat = ExerciseInfo(ExerciseId("squat"), "Squat", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)

internal fun done(id: String, kg: Double, reps: Int) = WorkoutSet(
    id = WorkoutSetId(id),
    entered = SetValues(weight = Weight(kg), reps = reps),
    isCompleted = true,
    completedAt = NOW,
)

internal fun exercise(id: String, info: ExerciseInfo, vararg sets: WorkoutSet) =
    WorkoutExercise(WorkoutExerciseId(id), info, sets = sets.toList())

/** A finished workout of one hour. */
internal fun finished(id: String, startedAt: Long, vararg exercises: WorkoutExercise, name: String? = null) = Workout(
    id = WorkoutId(id),
    name = name,
    startedAt = startedAt,
    endedAt = startedAt + HOUR,
    isInProgress = false,
    exercises = exercises.toList(),
)
