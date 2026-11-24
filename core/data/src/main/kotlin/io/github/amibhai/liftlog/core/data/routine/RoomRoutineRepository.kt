package io.github.amibhai.liftlog.core.data.routine

import androidx.room3.withWriteTransaction
import io.github.amibhai.liftlog.core.data.workout.toDomain
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.dao.HistoryDao
import io.github.amibhai.liftlog.core.database.dao.RoutineDao
import io.github.amibhai.liftlog.core.database.dao.RoutineExerciseGraph
import io.github.amibhai.liftlog.core.database.dao.RoutineGraph
import io.github.amibhai.liftlog.core.database.entity.RoutineEntity
import io.github.amibhai.liftlog.core.database.entity.RoutineExerciseEntity
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.RoutineSummary
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

internal class RoomRoutineRepository @Inject constructor(
    private val database: LiftLogDatabase,
    private val dao: RoutineDao,
    private val history: HistoryDao,
    private val clock: Clock,
) : RoutineRepository {

    override fun observeAll(): Flow<List<RoutineSummary>> =
        combine(dao.observeAll(), history.observeRoutineUse()) { graphs, uses ->
            val lastUse = uses.associate { it.routineId to it.at }
            graphs.map { RoutineSummary(it.toDomain(), lastUse[it.routine.id]) }
        }.distinctUntilChanged()

    override fun observe(id: RoutineId): Flow<Routine?> =
        dao.observe(id.value).map { it?.toDomain() }.distinctUntilChanged()

    override suspend fun get(id: RoutineId): Routine? = dao.load(id.value)?.toDomain()

    override suspend fun create(routine: Routine) {
        database.withWriteTransaction { write(stored = null, after = routine) }
    }

    override suspend fun edit(id: RoutineId, change: (Routine) -> Routine): Routine? = database.withWriteTransaction {
        dao.load(id.value)?.let { stored -> change(stored.toDomain()).also { write(stored, it) } }
    }

    override suspend fun delete(id: RoutineId) {
        val now = clock.millis()
        dao.setDeleted(id.value, deletedAt = now, now = now)
    }

    override suspend fun restore(id: RoutineId) {
        dao.setDeleted(id.value, deletedAt = null, now = clock.millis())
    }

    override suspend fun reorder(ids: List<RoutineId>) {
        val now = clock.millis()
        database.withWriteTransaction {
            ids.forEachIndexed { position, id -> dao.setPosition(id.value, position, now) }
        }
    }

    /** Writes only the rows that differ from [stored]; rows that are gone are soft-deleted. */
    private suspend fun write(stored: RoutineGraph?, after: Routine) {
        val now = clock.millis()
        val routine = RoutineEntity(
            id = after.id.value,
            name = after.name,
            notes = after.notes,
            position = stored?.routine?.position ?: dao.nextPosition(),
            createdAt = stored?.routine?.createdAt ?: now,
            updatedAt = now,
            deletedAt = null,
        )
        if (routine != stored?.routine?.copy(updatedAt = now)) dao.upsertRoutine(routine)

        val storedExercises = stored?.exercises.orEmpty().associate { it.routineExercise.id to it.routineExercise }
        val exercises = after.exercises.mapIndexed { position, exercise ->
            exercise.toEntity(after.id, position, storedExercises[exercise.id.value], now)
        }
        dao.upsertExercises(exercises.filter { it != storedExercises[it.id]?.copy(updatedAt = now) })

        val gone =
            storedExercises.values.filter { it.deletedAt == null }.map { it.id } - exercises.map { it.id }.toSet()
        if (gone.isNotEmpty()) dao.softDeleteExercises(gone, now)
    }
}

private fun RoutineGraph.toDomain() = Routine(
    id = RoutineId(routine.id),
    name = routine.name,
    notes = routine.notes,
    exercises = exercises.filter { it.routineExercise.deletedAt == null }
        .sortedBy { it.routineExercise.position }
        .map { it.toDomain() },
)

private fun RoutineExerciseGraph.toDomain() = RoutineExercise(
    id = RoutineExerciseId(routineExercise.id),
    exercise = exercise.toDomain(),
    supersetGroup = routineExercise.supersetGroup,
    targetSets = routineExercise.targetSets,
    repMin = routineExercise.repMin,
    repMax = routineExercise.repMax,
    targetRpe = routineExercise.targetRpe,
    targetRir = routineExercise.targetRir,
    restSeconds = routineExercise.restSeconds,
    progressionRule = routineExercise.progressionRule,
    notes = routineExercise.notes,
)

/** The row for this exercise; what the domain does not model yet is kept from [stored]. */
private fun RoutineExercise.toEntity(routineId: RoutineId, position: Int, stored: RoutineExerciseEntity?, now: Long) =
    RoutineExerciseEntity(
        id = id.value,
        routineId = routineId.value,
        exerciseId = exercise.id.value,
        position = position,
        supersetGroup = supersetGroup,
        targetSets = targetSets,
        repMin = repMin,
        repMax = repMax,
        targetRpe = targetRpe,
        targetRir = targetRir,
        restSeconds = restSeconds,
        progressionRule = progressionRule,
        ruleParams = stored?.ruleParams,
        notes = notes,
        createdAt = stored?.createdAt ?: now,
        updatedAt = now,
        deletedAt = null,
    )
