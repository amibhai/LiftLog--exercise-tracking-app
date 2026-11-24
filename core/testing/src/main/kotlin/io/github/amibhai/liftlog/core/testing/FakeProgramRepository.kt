package io.github.amibhai.liftlog.core.testing

import io.github.amibhai.liftlog.core.domain.model.ActiveProgram
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeProgramRepository(vararg initial: Program) : ProgramRepository {
    private val stored = MutableStateFlow(initial.toList())
    val lastDay = MutableStateFlow<ProgramDayId?>(null)

    val all: List<Program> get() = stored.value

    override fun observeAll(): Flow<List<Program>> = stored

    override fun observe(id: ProgramId): Flow<Program?> = stored.map { all -> all.firstOrNull { it.id == id } }

    override fun observeActive(): Flow<ActiveProgram?> = combine(stored, lastDay) { all, last ->
        all.firstOrNull { it.isActive }?.let { ActiveProgram(it, last) }
    }

    override suspend fun create(program: Program) {
        stored.value += program
    }

    override suspend fun edit(id: ProgramId, change: (Program) -> Program): Program? {
        stored.value = stored.value.map { if (it.id == id) change(it) else it }
        return stored.value.firstOrNull { it.id == id }
    }

    override suspend fun activate(id: ProgramId?) {
        stored.value = stored.value.map { it.copy(isActive = it.id == id) }
    }

    override suspend fun delete(id: ProgramId) {
        stored.value = stored.value.filterNot { it.id == id }
    }
}
