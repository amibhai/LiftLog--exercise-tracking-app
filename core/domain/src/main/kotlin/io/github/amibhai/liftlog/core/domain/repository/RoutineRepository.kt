package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.RoutineSummary
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    /** In the user's order. */
    fun observeAll(): Flow<List<RoutineSummary>>

    fun observe(id: RoutineId): Flow<Routine?>

    suspend fun get(id: RoutineId): Routine?

    /** Adds the routine at the end of the list. */
    suspend fun create(routine: Routine)

    /** Applies a pure [change] to the stored routine in one transaction; null when there is no such routine. */
    suspend fun edit(id: RoutineId, change: (Routine) -> Routine): Routine?

    /** Deletes softly, so that [restore] can undo it. */
    suspend fun delete(id: RoutineId)

    suspend fun restore(id: RoutineId)

    suspend fun reorder(ids: List<RoutineId>)
}
