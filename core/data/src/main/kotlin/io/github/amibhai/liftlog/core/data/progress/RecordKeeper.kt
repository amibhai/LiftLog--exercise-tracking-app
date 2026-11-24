package io.github.amibhai.liftlog.core.data.progress

import androidx.room3.withWriteTransaction
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.dao.ProgressDao
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.progress.recordTimeline
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the cache of personal records in step with the history (SPEC §10: derived, can be rebuilt). */
@Singleton
internal class RecordKeeper @Inject constructor(
    private val database: LiftLogDatabase,
    private val dao: ProgressDao,
    private val settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    /** Works out the records of a few exercises again, after the workouts they were done in changed. */
    suspend fun rebuild(exercises: Set<String>) {
        if (exercises.isEmpty()) return
        val formula = settings.settings.first().e1rmFormula
        val now = clock.millis()
        val sets = dao.setsOf(exercises.toList()).groupBy { it.exerciseId }
        val records = dao.typesOf(exercises.toList()).flatMap { exercise ->
            recordTimeline(ExerciseId(exercise.id), exercise.type, sets[exercise.id].orEmpty().toSessions(), formula)
        }.map { it.toEntity(ids.newId(), now) }
        database.withWriteTransaction {
            dao.deleteRecords(exercises.toList())
            dao.insertRecords(records)
        }
    }

    suspend fun rebuildAll() = rebuild(dao.trainedExercises().toSet())

    /** For an install that has a history but no records yet. */
    suspend fun rebuildIfEmpty() {
        if (dao.recordCount() == 0) rebuildAll()
    }
}
