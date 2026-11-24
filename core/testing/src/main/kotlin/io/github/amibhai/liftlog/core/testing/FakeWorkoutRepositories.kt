package io.github.amibhai.liftlog.core.testing

import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.RoutineSummary
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkoutRepository(vararg initial: Workout) : WorkoutRepository {
    private val stored = MutableStateFlow(initial.associateBy { it.id })
    private val deleted = mutableMapOf<WorkoutId, Workout>()

    val active: Workout? get() = stored.value.values.firstOrNull { it.isInProgress }

    operator fun get(id: WorkoutId): Workout? = stored.value[id]

    override fun observeActive(): Flow<Workout?> = stored.map { all -> all.values.firstOrNull { it.isInProgress } }

    override fun observe(id: WorkoutId): Flow<Workout?> = stored.map { it[id] }

    override fun observeFinished(limit: Int): Flow<List<Workout>> = stored.map { all ->
        all.values.filterNot { it.isInProgress }.sortedByDescending { it.startedAt }.take(limit)
    }

    override fun observeFinishedCount(): Flow<Int> = observeFinished(Int.MAX_VALUE).map { it.size }

    override suspend fun create(workout: Workout) {
        stored.value += workout.id to workout
    }

    override suspend fun addFinished(workouts: List<Workout>): Int =
        workouts.filter { it.id !in stored.value }.onEach { create(it) }.size

    override suspend fun edit(id: WorkoutId, change: (Workout) -> Workout): Workout? =
        stored.value[id]?.let(change)?.also { create(it) }

    override suspend fun delete(id: WorkoutId) {
        stored.value[id]?.let { deleted[id] = it }
        stored.value -= id
    }

    override suspend fun restore(id: WorkoutId) {
        deleted.remove(id)?.let { create(it) }
    }
}

class FakeRoutineRepository(vararg initial: Routine) : RoutineRepository {
    private val stored = MutableStateFlow(initial.toList())
    private val deleted = mutableMapOf<RoutineId, Routine>()

    val all: List<Routine> get() = stored.value

    override fun observeAll(): Flow<List<RoutineSummary>> = stored.map { all -> all.map { RoutineSummary(it, null) } }

    override fun observe(id: RoutineId): Flow<Routine?> = stored.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun get(id: RoutineId): Routine? = stored.value.firstOrNull { it.id == id }

    override suspend fun create(routine: Routine) {
        stored.value += routine
    }

    override suspend fun edit(id: RoutineId, change: (Routine) -> Routine): Routine? {
        stored.value = stored.value.map { if (it.id == id) change(it) else it }
        return get(id)
    }

    override suspend fun delete(id: RoutineId) {
        get(id)?.let { deleted[id] = it }
        stored.value = stored.value.filterNot { it.id == id }
    }

    override suspend fun restore(id: RoutineId) {
        deleted.remove(id)?.let { create(it) }
    }

    override suspend fun reorder(ids: List<RoutineId>) {
        stored.value = ids.mapNotNull { id -> stored.value.firstOrNull { it.id == id } }
    }
}
