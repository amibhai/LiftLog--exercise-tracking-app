package io.github.amibhai.liftlog.core.domain.usecase

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.program.dayOf
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import kotlinx.coroutines.flow.first
import java.time.Clock

/**
 * The start options of the home screen (SPEC §5). There is only ever one workout in progress: while one
 * is, starting another returns it instead.
 */
class StartWorkout(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val programs: ProgramRepository,
    private val history: HistoryRepository,
    private val advisor: ProgressionAdvisor,
    private val factory: WorkoutFactory,
    private val clock: Clock,
) {

    suspend fun empty(): WorkoutId = start { factory.empty(it) }

    /** Null when the routine no longer exists. In a program that is followed, it counts for its day. */
    suspend fun fromRoutine(id: RoutineId): WorkoutId? {
        val routine = routines.get(id) ?: return null
        return start { now ->
            val latest = latest(routine.exercises.map { it.exercise.id }, now)
            factory.fromRoutine(routine, latest, advisor.advise(routine.exercises, now), now)
                .copy(programDayId = programs.observeActive().first()?.dayOf(id)?.id)
        }
    }

    /** The same exercises as an earlier workout; null when it no longer exists. */
    suspend fun repeat(id: WorkoutId): WorkoutId? {
        val old = workouts.observe(id).first() ?: return null
        return start { now -> factory.repeat(old, latest(old.exercises.map { it.exercise.id }, now), now) }
    }

    private suspend fun start(create: suspend (now: Long) -> Workout): WorkoutId {
        val active = workouts.observeActive().first()
        return active?.id ?: create(clock.millis()).also { workouts.create(it) }.id
    }

    private suspend fun latest(ids: List<ExerciseId>, now: Long): Map<ExerciseId, List<LoggedSet>> =
        history.latestSessions(before = now, ids = ids.toSet()).mapValues { it.value.sets }
}
