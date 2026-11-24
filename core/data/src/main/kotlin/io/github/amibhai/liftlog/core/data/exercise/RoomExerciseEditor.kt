package io.github.amibhai.liftlog.core.data.exercise

import androidx.room3.withWriteTransaction
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.search.SearchTerms
import java.time.Clock
import javax.inject.Inject

internal class RoomExerciseEditor @Inject constructor(
    private val database: LiftLogDatabase,
    private val dao: ExerciseDao,
    private val ids: IdGenerator,
    private val clock: Clock,
) : ExerciseEditor {

    override suspend fun save(draft: ExerciseDraft): ExerciseId = database.withWriteTransaction {
        val now = clock.millis()
        val stored = draft.id?.let { dao.get(it.value) }
        val entity = (stored ?: newCustomExercise(draft, now)).updatedWith(draft, now)
        if (stored == null) dao.insert(entity) else dao.update(entity)
        dao.upsertMuscles(musclesOf(entity.id, draft, now))
        ExerciseId(entity.id)
    }

    override suspend fun deleteCustom(id: ExerciseId) = dao.softDeleteCustom(id.value, clock.millis())

    override suspend fun setHidden(id: ExerciseId, hidden: Boolean) = dao.setArchived(id.value, hidden, clock.millis())

    override suspend fun setFavorite(id: ExerciseId, favorite: Boolean) =
        dao.setFavorite(id.value, favorite, clock.millis())

    override suspend fun setStickyNote(id: ExerciseId, note: String?) =
        dao.setNotes(id.value, note?.trim()?.ifEmpty { null }, clock.millis())

    override suspend fun setDefaults(id: ExerciseId, defaults: ExerciseDefaults) = dao.setDefaults(
        id = id.value,
        restSeconds = defaults.restSeconds,
        stepKg = defaults.weightStepKg,
        stepLb = defaults.weightStepLb,
        barKg = defaults.barWeight?.kg,
        now = clock.millis(),
    )

    private fun ExerciseEntity.updatedWith(draft: ExerciseDraft, now: Long) = copy(
        name = draft.name.trim(),
        searchTerms = (searchTerms.lines() + SearchTerms.of(draft.equipment, draft.primaryMuscles))
            .filter { it.isNotBlank() }.distinct().sorted().joinToString("\n"),
        type = draft.type,
        equipment = draft.equipment,
        category = draft.category,
        instructions = draft.instructions.filter { it.isNotBlank() }.joinToString("\n") { it.trim() },
        isUnilateral = draft.isUnilateral,
        userModifiedAt = now,
        updatedAt = now,
    )

    private fun newCustomExercise(draft: ExerciseDraft, now: Long) = ExerciseEntity(
        id = ids.newId(),
        name = draft.name,
        aliases = "",
        searchTerms = "",
        type = draft.type,
        equipment = draft.equipment,
        category = draft.category,
        mechanic = null,
        instructions = "",
        isUnilateral = draft.isUnilateral,
        defaultRestSeconds = null,
        weightStepKg = null,
        weightStepLb = null,
        barWeightKg = null,
        notes = null,
        isFavorite = false,
        isArchived = false,
        isCustom = true,
        popularityRank = null,
        sourceRef = null,
        license = null,
        licenseUrl = null,
        licenseAuthor = null,
        libraryVersion = null,
        userModifiedAt = now,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
    )

    /** The rows that make up the draft's muscles, reviving or soft-deleting the stored ones. */
    private suspend fun musclesOf(id: String, draft: ExerciseDraft, now: Long): List<ExerciseMuscleEntity> {
        val stored = dao.musclesOf(id).associateBy { it.muscle }
        val wanted = draft.secondaryMuscles.associateWith { false } + draft.primaryMuscles.associateWith { true }
        val kept = wanted.map { (muscle, isPrimary) ->
            val row = stored[muscle] ?: ExerciseMuscleEntity(
                ids.newId(),
                id,
                muscle,
                isPrimary,
                now,
                now,
                deletedAt = null,
            )
            row.copy(isPrimary = isPrimary, updatedAt = now, deletedAt = null)
        }
        val gone = stored.values.filter { it.muscle !in wanted && it.deletedAt == null }
        return kept + gone.map { it.copy(updatedAt = now, deletedAt = now) }
    }
}
