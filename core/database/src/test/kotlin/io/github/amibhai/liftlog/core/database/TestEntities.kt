package io.github.amibhai.liftlog.core.database

import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseImageEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle

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
