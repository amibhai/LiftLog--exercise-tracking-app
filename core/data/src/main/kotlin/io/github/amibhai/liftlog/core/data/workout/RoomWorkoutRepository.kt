package io.github.amibhai.liftlog.core.data.workout

import androidx.room3.withWriteTransaction
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.dao.WorkoutDao
import io.github.amibhai.liftlog.core.database.dao.WorkoutGraph
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

internal class RoomWorkoutRepository @Inject constructor(
    private val database: LiftLogDatabase,
    private val dao: WorkoutDao,
    private val clock: Clock,
) : WorkoutRepository {

    override fun observeActive(): Flow<Workout?> = dao.observeActive().map { it?.toDomain() }.distinctUntilChanged()

    override fun observe(id: WorkoutId): Flow<Workout?> =
        dao.observe(id.value).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeFinished(limit: Int): Flow<List<Workout>> =
        dao.observeFinished(limit).map { graphs -> graphs.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeFinishedCount(): Flow<Int> = dao.observeFinishedCount().distinctUntilChanged()

    override suspend fun create(workout: Workout) {
        database.withWriteTransaction { write(stored = null, after = workout) }
    }

    override suspend fun edit(id: WorkoutId, change: (Workout) -> Workout): Workout? = database.withWriteTransaction {
        dao.load(id.value)?.let { stored -> change(stored.toDomain()).also { write(stored, it) } }
    }

    override suspend fun delete(id: WorkoutId) {
        val now = clock.millis()
        dao.setDeleted(id.value, deletedAt = now, now = now)
    }

    override suspend fun restore(id: WorkoutId) {
        dao.setDeleted(id.value, deletedAt = null, now = clock.millis())
    }

    /** Writes only the rows that differ from [stored]; rows that are gone are soft-deleted. */
    private suspend fun write(stored: WorkoutGraph?, after: Workout) {
        val now = clock.millis()
        val workout = after.toEntity(stored?.workout, now)
        if (workout != stored?.workout?.copy(updatedAt = now)) dao.upsertWorkout(workout)

        val storedExercises = stored?.exercises.orEmpty().associate { it.workoutExercise.id to it.workoutExercise }
        val exercises = after.exercises.mapIndexed { position, exercise ->
            exercise.toEntity(after.id, position, storedExercises[exercise.id.value]?.createdAt, now)
        }
        dao.upsertExercises(exercises.filter { it != storedExercises[it.id]?.copy(updatedAt = now) })

        val storedSets = stored?.exercises.orEmpty().flatMap { it.sets }.associateBy { it.id }
        val sets = after.exercises.flatMap { exercise ->
            exercise.sets.mapIndexed { position, set ->
                set.toEntity(exercise.id, position, storedSets[set.id.value]?.createdAt, now)
            }
        }
        dao.upsertSets(sets.filter { it != storedSets[it.id]?.copy(updatedAt = now) })

        val goneExercises =
            storedExercises.values.filter { it.deletedAt == null }.map { it.id } - exercises.map { it.id }.toSet()
        if (goneExercises.isNotEmpty()) dao.softDeleteExercises(goneExercises, now)
        // The sets of a removed exercise stay with it, so that undo brings both back.
        val goneSets = storedSets.values
            .filter { it.deletedAt == null && it.workoutExerciseId !in goneExercises }
            .map { it.id } - sets.map { it.id }.toSet()
        if (goneSets.isNotEmpty()) dao.softDeleteSets(goneSets, now)
    }
}
