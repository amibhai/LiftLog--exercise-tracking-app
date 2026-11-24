package io.github.amibhai.liftlog.core.data.workout

import androidx.room3.withWriteTransaction
import io.github.amibhai.liftlog.core.data.progress.RecordKeeper
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
    private val records: RecordKeeper,
) : WorkoutRepository {

    override fun observeActive(): Flow<Workout?> = dao.observeActive().map { it?.toDomain() }.distinctUntilChanged()

    override fun observe(id: WorkoutId): Flow<Workout?> =
        dao.observe(id.value).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeFinished(limit: Int): Flow<List<Workout>> =
        dao.observeFinished(limit).map { graphs -> graphs.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeFinishedCount(): Flow<Int> = dao.observeFinishedCount().distinctUntilChanged()

    override suspend fun create(workout: Workout) {
        database.withWriteTransaction { write(stored = null, after = workout) }
        records.rebuild(workout.finishedExercises)
    }

    override suspend fun addFinished(workouts: List<Workout>): Int {
        var added = 0
        // In parts, so that a long history does not keep the database busy for too long at once.
        for (part in workouts.chunked(IMPORT_PART)) {
            database.withWriteTransaction {
                val known = dao.knownIds(part.map { it.id.value }).toSet()
                val new = part.filter { it.id.value !in known }
                new.forEach { write(stored = null, after = it) }
                added += new.size
            }
        }
        records.rebuild(workouts.flatMapTo(mutableSetOf()) { it.finishedExercises })
        return added
    }

    override suspend fun edit(id: WorkoutId, change: (Workout) -> Workout): Workout? {
        val edited: Pair<Workout, Workout>? = database.withWriteTransaction {
            dao.load(id.value)?.let { stored ->
                val before = stored.toDomain()
                before to change(before).also { write(stored, it) }
            }
        }
        edited?.let { (before, after) -> records.rebuild(before.finishedExercises + after.finishedExercises) }
        return edited?.second
    }

    override suspend fun delete(id: WorkoutId) {
        val deleted = dao.load(id.value)?.toDomain()
        val now = clock.millis()
        dao.setDeleted(id.value, deletedAt = now, now = now)
        records.rebuild(deleted?.finishedExercises.orEmpty())
    }

    override suspend fun restore(id: WorkoutId) {
        dao.setDeleted(id.value, deletedAt = null, now = clock.millis())
        records.rebuild(dao.load(id.value)?.toDomain()?.finishedExercises.orEmpty())
    }

    /** Records only follow finished workouts. */
    private val Workout.finishedExercises: Set<String>
        get() = if (isInProgress) emptySet() else exercises.mapTo(mutableSetOf()) { it.exercise.id.value }

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

    private companion object {
        const val IMPORT_PART = 50
    }
}
