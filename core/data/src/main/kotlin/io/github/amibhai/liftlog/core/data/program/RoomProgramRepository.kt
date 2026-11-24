package io.github.amibhai.liftlog.core.data.program

import androidx.room3.withWriteTransaction
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.dao.ProgramDao
import io.github.amibhai.liftlog.core.database.dao.ProgramGraph
import io.github.amibhai.liftlog.core.database.entity.ProgramDayEntity
import io.github.amibhai.liftlog.core.database.entity.ProgramEntity
import io.github.amibhai.liftlog.core.domain.model.ActiveProgram
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

internal class RoomProgramRepository @Inject constructor(
    private val database: LiftLogDatabase,
    private val dao: ProgramDao,
    private val clock: Clock,
) : ProgramRepository {

    override fun observeAll(): Flow<List<Program>> =
        dao.observeAll().map { graphs -> graphs.map { it.toDomain() } }.distinctUntilChanged()

    override fun observe(id: ProgramId): Flow<Program?> =
        dao.observe(id.value).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeActive(): Flow<ActiveProgram?> =
        combine(observeAll(), dao.observeLastDay()) { programs, lastDay ->
            programs.firstOrNull { it.isActive }?.let { ActiveProgram(it, lastDay?.let(::ProgramDayId)) }
        }.distinctUntilChanged()

    override suspend fun create(program: Program) {
        database.withWriteTransaction { write(stored = null, after = program) }
    }

    override suspend fun edit(id: ProgramId, change: (Program) -> Program): Program? = database.withWriteTransaction {
        dao.load(id.value)?.let { stored -> change(stored.toDomain()).also { write(stored, it) } }
    }

    override suspend fun activate(id: ProgramId?) = dao.activate(id?.value, clock.millis())

    override suspend fun delete(id: ProgramId) = dao.softDelete(id.value, clock.millis())

    private suspend fun write(stored: ProgramGraph?, after: Program) {
        val now = clock.millis()
        dao.upsertProgram(
            ProgramEntity(
                id = after.id.value,
                name = after.name,
                description = stored?.program?.description,
                templateKey = after.templateKey,
                isActive = after.isActive,
                createdAt = stored?.program?.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        val storedDays = stored?.days.orEmpty().map { it.day }.associateBy { it.id }
        dao.upsertDays(
            after.days.mapIndexed { position, day ->
                ProgramDayEntity(
                    id = day.id.value,
                    programId = after.id.value,
                    position = position,
                    name = day.name,
                    // A deleted routine can come back with undo, and its day with it.
                    routineId = day.routineId?.value ?: storedDays[day.id.value]?.routineId,
                    createdAt = storedDays[day.id.value]?.createdAt ?: now,
                    updatedAt = now,
                    deletedAt = null,
                )
            },
        )
        val gone =
            storedDays.values.filter { it.deletedAt == null }.map { it.id } - after.days.map { it.id.value }.toSet()
        if (gone.isNotEmpty()) dao.softDeleteDays(gone, now)
    }
}

private fun ProgramGraph.toDomain() = Program(
    id = ProgramId(program.id),
    name = program.name,
    templateKey = program.templateKey,
    isActive = program.isActive,
    days = days.filter { it.day.deletedAt == null }
        .sortedBy { it.day.position }
        .map { graph ->
            ProgramDay(
                id = ProgramDayId(graph.day.id),
                name = graph.day.name,
                routineId = graph.routine?.takeIf { it.deletedAt == null }?.let { RoutineId(it.id) },
            )
        },
)
