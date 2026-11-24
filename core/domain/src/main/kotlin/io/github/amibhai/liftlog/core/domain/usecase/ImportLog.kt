package io.github.amibhai.liftlog.core.domain.usecase

import io.github.amibhai.liftlog.core.domain.importer.ExerciseMatcher
import io.github.amibhai.liftlog.core.domain.importer.ImportedLog
import io.github.amibhai.liftlog.core.domain.importer.draftFor
import io.github.amibhai.liftlog.core.domain.importer.toWorkouts
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/** What a name in the file probably stands for; null when nothing in the library is close. */
data class NameMatch(val name: String, val exercise: CatalogExercise?, val isSure: Boolean)

/** What to do with a name in the file. */
sealed interface ImportChoice {
    data class Existing(val id: ExerciseId) : ImportChoice

    /** A custom exercise with that name. */
    data object Create : ImportChoice

    /** Its sets are left out. */
    data object Skip : ImportChoice
}

data class ImportResult(val added: Int, val alreadyThere: Int)

/** Brings the training log of another app in (SPEC §9). */
class ImportLog(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val editor: ExerciseEditor,
) {
    /** The most frequent names first. */
    suspend fun matches(log: ImportedLog): List<NameMatch> {
        val matcher = ExerciseMatcher(exercises.observeCatalog().first())
        return log.exerciseNames.map { name ->
            val match = matcher.match(name)
            NameMatch(name, match?.exercise, isSure = match?.isSure ?: false)
        }
    }

    /** Importing the same file twice adds nothing the second time. */
    suspend fun import(
        log: ImportedLog,
        choices: Map<String, ImportChoice>,
        unit: WeightUnit,
        zone: ZoneId,
    ): ImportResult {
        val ids = choices.mapNotNull { (name, choice) ->
            when (choice) {
                is ImportChoice.Existing -> name to choice.id
                ImportChoice.Create -> name to editor.save(log.draftFor(name))
                ImportChoice.Skip -> null
            }
        }.toMap()
        val infos = exercises.infos(ids.values.distinct()).associateBy { it.id }
        val byName = ids.mapNotNull { (name, id) -> infos[id]?.let { name to it } }.toMap()
        val imported = log.toWorkouts(byName, unit, zone)
        val added = workouts.addFinished(imported)
        return ImportResult(added, alreadyThere = imported.size - added)
    }
}
