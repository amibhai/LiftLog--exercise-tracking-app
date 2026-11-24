package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.ActiveProgram
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import kotlinx.coroutines.flow.Flow

/** Rotations of routines (SPEC §7). At most one is followed at a time. */
interface ProgramRepository {
    /** The followed one first, then by age. */
    fun observeAll(): Flow<List<Program>>

    fun observe(id: ProgramId): Flow<Program?>

    /** The program that is followed, or null. */
    fun observeActive(): Flow<ActiveProgram?>

    suspend fun create(program: Program)

    /** Applies a pure [change] to the stored program in one transaction; null when there is no such program. */
    suspend fun edit(id: ProgramId, change: (Program) -> Program): Program?

    /** Follows this program and no other; null follows none. */
    suspend fun activate(id: ProgramId?)

    /** Its routines stay. */
    suspend fun delete(id: ProgramId)
}
