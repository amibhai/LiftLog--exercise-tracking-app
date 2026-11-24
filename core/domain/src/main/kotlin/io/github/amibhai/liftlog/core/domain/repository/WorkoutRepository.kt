package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    /** The workout in progress, or null. It is stored on every change, so it survives process death (SPEC §5). */
    fun observeActive(): Flow<Workout?>

    fun observe(id: WorkoutId): Flow<Workout?>

    /** The [limit] most recent finished workouts, newest first. */
    fun observeFinished(limit: Int): Flow<List<Workout>>

    fun observeFinishedCount(): Flow<Int>

    suspend fun create(workout: Workout)

    /** Adds finished workouts, except for those that are or were there already; returns how many were added. */
    suspend fun addFinished(workouts: List<Workout>): Int

    /** Applies a pure [change] to the stored workout in one transaction; null when there is no such workout. */
    suspend fun edit(id: WorkoutId, change: (Workout) -> Workout): Workout?

    /** Deletes softly, so that [restore] can undo it. */
    suspend fun delete(id: WorkoutId)

    suspend fun restore(id: WorkoutId)
}
