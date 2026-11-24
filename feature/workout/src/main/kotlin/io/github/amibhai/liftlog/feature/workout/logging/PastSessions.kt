package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.progress.Bests
import io.github.amibhai.liftlog.core.domain.progress.E1rmFormula
import io.github.amibhai.liftlog.core.domain.progress.bestsOf
import io.github.amibhai.liftlog.core.domain.progression.Advice
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.usecase.ProgressionAdvisor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import java.time.Clock
import javax.inject.Inject

/** What was done before the workout on screen. */
internal data class Past(
    /** The last time each exercise was done: the "Previous" column. */
    val previous: Map<ExerciseId, List<LoggedSet>> = emptyMap(),
    /** The records to beat. */
    val bests: Map<ExerciseId, Bests> = emptyMap(),
    /** Why the sets are planned as they are. */
    val advice: Map<RoutineExerciseId, Advice> = emptyMap(),
)

/** Looks up the [Past]. It does not change while a workout is logged, so it is read once per set of exercises. */
class PastSessions @Inject constructor(
    private val history: HistoryRepository,
    private val progress: ProgressRepository,
    private val routines: RoutineRepository,
    private val advisor: ProgressionAdvisor,
    private val clock: Clock,
) {
    internal fun observe(workout: Flow<Workout>, formula: Flow<E1rmFormula>): Flow<Past> = combine(
        previous(workout).onStart { emit(emptyMap()) },
        bests(workout, formula).onStart { emit(emptyMap()) },
        advice(workout).onStart { emit(emptyMap()) },
        ::Past,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun previous(workout: Flow<Workout>): Flow<Map<ExerciseId, List<LoggedSet>>> = workout
        .map { it.exercises.mapTo(mutableSetOf()) { exercise -> exercise.exercise.id } to it.editedAt }
        .distinctUntilChanged()
        .mapLatest { (ids, before) ->
            history.latestSessions(before ?: clock.millis(), ids).mapValues { it.value.sets }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun bests(workout: Flow<Workout>, formula: Flow<E1rmFormula>): Flow<Map<ExerciseId, Bests>> = combine(
        workout
            .map {
                it.exercises.associate { exercise -> exercise.exercise.id to exercise.exercise.type } to it.editedAt
            }
            .distinctUntilChanged(),
        formula.distinctUntilChanged(),
        ::Pair,
    ).mapLatest { (exercises, formula) ->
        val (types, before) = exercises
        progress.sessionsBefore(before ?: clock.millis(), types.keys)
            .mapValues { (id, sessions) -> bestsOf(types.getValue(id), sessions, formula) }
    }

    /** Only while the workout is in progress: afterwards there is nothing left to explain. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun advice(workout: Flow<Workout>): Flow<Map<RoutineExerciseId, Advice>> = workout
        .map { it.routineId?.takeIf { _ -> it.isInProgress } to it.startedAt }
        .distinctUntilChanged()
        .mapLatest { (routine, before) ->
            advisor.advise(routine?.let { routines.get(it) }?.exercises.orEmpty(), before)
        }

    /** An earlier workout is compared with what came before it; the current one with everything. */
    private val Workout.editedAt: Long? get() = startedAt.takeUnless { isInProgress }
}
