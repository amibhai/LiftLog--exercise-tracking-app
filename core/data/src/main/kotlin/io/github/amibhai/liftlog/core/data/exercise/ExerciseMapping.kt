package io.github.amibhai.liftlog.core.data.exercise

import io.github.amibhai.liftlog.core.database.dao.CatalogRow
import io.github.amibhai.liftlog.core.database.dao.ExerciseWithDetails
import io.github.amibhai.liftlog.core.database.dao.PrimaryMuscleRow
import io.github.amibhai.liftlog.core.database.dao.ThumbnailRow
import io.github.amibhai.liftlog.core.database.entity.LibrarySourceEntity
import io.github.amibhai.liftlog.core.domain.model.Attribution
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseImage
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.units.Weight

/** Source-ref prefixes written by tools/exercise-pipeline, mapped to the names shown in the app. */
private val SOURCE_NAMES = mapOf("fedb" to "free-exercise-db", "wger" to "wger")

internal fun toCatalog(
    rows: List<CatalogRow>,
    muscles: List<PrimaryMuscleRow>,
    thumbnails: List<ThumbnailRow>,
): List<CatalogExercise> {
    val musclesById = muscles.groupBy({ it.exerciseId }, { it.muscle })
    val thumbnailById = thumbnails.associate { it.exerciseId to it.url }
    return rows.map { row ->
        CatalogExercise(
            id = ExerciseId(row.id),
            name = row.name,
            aliases = row.aliases.lines().filter { it.isNotBlank() },
            searchTerms = row.searchTerms.lines().filter { it.isNotBlank() },
            type = row.type,
            equipment = row.equipment,
            primaryMuscles = musclesById[row.id].orEmpty(),
            thumbnailUrl = thumbnailById[row.id],
            popularityRank = row.popularityRank,
            isCustom = row.isCustom,
            isFavorite = row.isFavorite,
        )
    }
}

internal fun ExerciseWithDetails.toDetail(): ExerciseDetail {
    val liveMuscles = muscles.filter { it.deletedAt == null }
    return ExerciseDetail(
        id = ExerciseId(exercise.id),
        name = exercise.name,
        aliases = exercise.aliases.lines().filter { it.isNotBlank() },
        type = exercise.type,
        equipment = exercise.equipment,
        category = exercise.category,
        mechanic = exercise.mechanic,
        primaryMuscles = liveMuscles.filter { it.isPrimary }.map { it.muscle },
        secondaryMuscles = liveMuscles.filterNot { it.isPrimary }.map { it.muscle },
        instructions = exercise.instructions.lines().filter { it.isNotBlank() },
        images = images.filter { it.deletedAt == null }.sortedBy { it.position }.map {
            ExerciseImage(url = it.url, thumbnailUrl = it.thumbnailUrl, license = it.license, author = it.author)
        },
        isUnilateral = exercise.isUnilateral,
        isCustom = exercise.isCustom,
        isFavorite = exercise.isFavorite,
        isArchived = exercise.isArchived,
        notes = exercise.notes,
        defaults = ExerciseDefaults(
            restSeconds = exercise.defaultRestSeconds,
            weightStepKg = exercise.weightStepKg,
            weightStepLb = exercise.weightStepLb,
            barWeight = exercise.barWeightKg?.let(::Weight),
        ),
        attribution = exercise.sourceRef?.let { ref ->
            Attribution(
                sourceName = SOURCE_NAMES[ref.substringBefore(':')] ?: ref.substringBefore(':'),
                license = exercise.license,
                licenseUrl = exercise.licenseUrl,
                author = exercise.licenseAuthor,
            )
        },
    )
}

internal fun LibrarySourceEntity.toDomain() = LibrarySource(
    name = name,
    url = url,
    license = license,
    licenseUrl = licenseUrl,
    revision = revision,
    notice = notice,
    exerciseCount = exerciseCount,
)
