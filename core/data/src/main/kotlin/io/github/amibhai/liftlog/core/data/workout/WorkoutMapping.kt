package io.github.amibhai.liftlog.core.data.workout

import io.github.amibhai.liftlog.core.database.dao.ExerciseInfoRow
import io.github.amibhai.liftlog.core.database.dao.WorkoutExerciseGraph
import io.github.amibhai.liftlog.core.database.dao.WorkoutGraph
import io.github.amibhai.liftlog.core.database.entity.WorkoutEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutSetEntity
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.RestTimer
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight

internal fun ExerciseInfoRow.toDomain() = ExerciseInfo(
    id = ExerciseId(id),
    name = name,
    type = type,
    equipment = equipment,
    stickyNote = notes,
    defaults = ExerciseDefaults(
        restSeconds = defaultRestSeconds,
        weightStepKg = weightStepKg,
        weightStepLb = weightStepLb,
        barWeight = barWeightKg?.let(::Weight),
    ),
)

internal fun WorkoutGraph.toDomain() = Workout(
    id = WorkoutId(workout.id),
    name = workout.name,
    startedAt = workout.startedAt,
    endedAt = workout.endedAt,
    routineId = workout.routineId?.let(::RoutineId),
    programDayId = workout.programDayId?.let(::ProgramDayId),
    notes = workout.notes,
    isInProgress = workout.isInProgress,
    restTimer = workout.restTimerEndsAt?.let { RestTimer(it, workout.restTimerTotalSeconds ?: 0) },
    exercises = exercises.filter { it.workoutExercise.deletedAt == null }
        .sortedBy { it.workoutExercise.position }
        .map { it.toDomain() },
)

private fun WorkoutExerciseGraph.toDomain() = WorkoutExercise(
    id = WorkoutExerciseId(workoutExercise.id),
    exercise = exercise.toDomain(),
    supersetGroup = workoutExercise.supersetGroup,
    notes = workoutExercise.notes,
    restSeconds = workoutExercise.restSeconds,
    routineExerciseId = workoutExercise.routineExerciseId?.let(::RoutineExerciseId),
    sets = sets.filter { it.deletedAt == null }.sortedBy { it.position }.map { it.toDomain() },
)

private fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = WorkoutSetId(id),
    type = type,
    entered = SetValues(weightKg?.let(::Weight), reps, durationSeconds, distanceMeters),
    target = SetValues(targetWeightKg?.let(::Weight), targetReps, targetDurationSeconds, targetDistanceMeters),
    rpe = rpe,
    rir = rir,
    isCompleted = isCompleted,
    completedAt = completedAt,
)

/** The row for this workout; what the domain does not model is kept from [stored]. */
internal fun Workout.toEntity(stored: WorkoutEntity?, now: Long) = WorkoutEntity(
    id = id.value,
    name = name,
    startedAt = startedAt,
    endedAt = endedAt,
    routineId = routineId?.value,
    programDayId = programDayId?.value,
    notes = notes,
    isInProgress = isInProgress,
    bodyweightKg = stored?.bodyweightKg,
    restTimerEndsAt = restTimer?.endsAt,
    restTimerTotalSeconds = restTimer?.totalSeconds,
    createdAt = stored?.createdAt ?: now,
    updatedAt = now,
    deletedAt = null,
)

internal fun WorkoutExercise.toEntity(workoutId: WorkoutId, position: Int, createdAt: Long?, now: Long) =
    WorkoutExerciseEntity(
        id = id.value,
        workoutId = workoutId.value,
        exerciseId = exercise.id.value,
        position = position,
        supersetGroup = supersetGroup,
        notes = notes,
        routineExerciseId = routineExerciseId?.value,
        restSeconds = restSeconds,
        createdAt = createdAt ?: now,
        updatedAt = now,
        deletedAt = null,
    )

internal fun WorkoutSet.toEntity(exerciseId: WorkoutExerciseId, position: Int, createdAt: Long?, now: Long) =
    WorkoutSetEntity(
        id = id.value,
        workoutExerciseId = exerciseId.value,
        position = position,
        type = type,
        weightKg = entered.weight?.kg,
        reps = entered.reps,
        durationSeconds = entered.durationSeconds,
        distanceMeters = entered.distanceMeters,
        rpe = rpe,
        rir = rir,
        targetWeightKg = target.weight?.kg,
        targetReps = target.reps,
        targetDurationSeconds = target.durationSeconds,
        targetDistanceMeters = target.distanceMeters,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt ?: now,
        updatedAt = now,
        deletedAt = null,
    )
