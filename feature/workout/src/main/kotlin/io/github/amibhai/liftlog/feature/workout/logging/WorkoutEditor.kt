package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import javax.inject.Inject

/**
 * Changes stored workouts, one change after the other and independent of any screen's lifetime, so that
 * nothing typed is ever lost (SPEC §5: crash-proof).
 */
class WorkoutEditor @Inject constructor(
    private val workouts: WorkoutRepository,
    private val work: SequentialWork,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    fun observe(id: WorkoutId): Flow<Workout?> = workouts.observe(id)

    fun edit(id: WorkoutId, change: (Workout) -> Workout) = work.submit { workouts.edit(id, change) }

    fun delete(id: WorkoutId) = work.submit { workouts.delete(id) }

    fun submit(task: suspend () -> Unit) = work.submit(task)

    fun newId(): String = ids.newId()

    fun now(): Long = clock.millis()
}
