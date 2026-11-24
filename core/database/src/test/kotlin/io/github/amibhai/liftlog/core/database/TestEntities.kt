package io.github.amibhai.liftlog.core.database

import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseImageEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutSetEntity
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.SetType

internal const val NOW = 1_700_000_000_000L

internal fun exercise(
    name: String,
    id: String = "id-" + name.lowercase().replace(' ', '-'),
    aliases: List<String> = emptyList(),
    searchTerms: List<String> = emptyList(),
    equipment: Equipment = Equipment.BARBELL,
    popularityRank: Int? = null,
    isCustom: Boolean = false,
    isArchived: Boolean = false,
    deletedAt: Long? = null,
) = ExerciseEntity(
    id = id,
    name = name,
    aliases = aliases.joinToString("\n"),
    searchTerms = searchTerms.joinToString("\n"),
    type = ExerciseType.WEIGHT_REPS,
    equipment = equipment,
    category = ExerciseCategory.STRENGTH,
    mechanic = null,
    instructions = "",
    isUnilateral = false,
    defaultRestSeconds = null,
    weightStepKg = null,
    weightStepLb = null,
    barWeightKg = null,
    notes = null,
    isFavorite = false,
    isArchived = isArchived,
    isCustom = isCustom,
    popularityRank = popularityRank,
    sourceRef = if (isCustom) null else "test:$id",
    license = null,
    licenseUrl = null,
    licenseAuthor = null,
    libraryVersion = null,
    userModifiedAt = null,
    createdAt = NOW,
    updatedAt = NOW,
    deletedAt = deletedAt,
)

internal fun muscle(exerciseId: String, muscle: Muscle, isPrimary: Boolean) = ExerciseMuscleEntity(
    id = "$exerciseId-${muscle.name}",
    exerciseId = exerciseId,
    muscle = muscle,
    isPrimary = isPrimary,
    createdAt = NOW,
    updatedAt = NOW,
    deletedAt = null,
)

internal fun image(exerciseId: String, position: Int, url: String) = ExerciseImageEntity(
    id = "$exerciseId-img-$position",
    exerciseId = exerciseId,
    position = position,
    url = url,
    thumbnailUrl = null,
    license = "CC0 1.0",
    author = "someone",
    createdAt = NOW,
    updatedAt = NOW,
    deletedAt = null,
)

internal fun workout(id: String, startedAt: Long, inProgress: Boolean = false, routineId: String? = null) =
    WorkoutEntity(
        id = id,
        name = null,
        startedAt = startedAt,
        endedAt = if (inProgress) null else startedAt + 3_600_000,
        routineId = routineId,
        programDayId = null,
        notes = null,
        isInProgress = inProgress,
        bodyweightKg = null,
        restTimerEndsAt = null,
        restTimerTotalSeconds = null,
        createdAt = startedAt,
        updatedAt = startedAt,
        deletedAt = null,
    )

internal fun workoutExercise(id: String, workoutId: String, exerciseId: String, position: Int) = WorkoutExerciseEntity(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    supersetGroup = null,
    notes = null,
    routineExerciseId = null,
    restSeconds = null,
    createdAt = NOW,
    updatedAt = NOW,
    deletedAt = null,
)

internal fun workoutSet(
    id: String,
    workoutExerciseId: String,
    position: Int,
    type: SetType,
    weightKg: Double?,
    reps: Int?,
    isCompleted: Boolean,
) = WorkoutSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    type = type,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = null,
    distanceMeters = null,
    rpe = null,
    rir = null,
    targetWeightKg = null,
    targetReps = null,
    targetDurationSeconds = null,
    targetDistanceMeters = null,
    isCompleted = isCompleted,
    completedAt = if (isCompleted) NOW else null,
    createdAt = NOW,
    updatedAt = NOW,
    deletedAt = null,
)
