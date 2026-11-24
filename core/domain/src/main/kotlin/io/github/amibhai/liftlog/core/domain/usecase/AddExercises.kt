package io.github.amibhai.liftlog.core.domain.usecase

import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import io.github.amibhai.liftlog.core.domain.workout.addExercises
import io.github.amibhai.liftlog.core.domain.workout.editExercise
import io.github.amibhai.liftlog.core.domain.workout.replaceExercise
import java.time.Clock

/** What the exercise picker does with its selection (SPEC §4). */
class AddExercises(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val history: HistoryRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    private val factory = WorkoutFactory(ids)

    /** Adds exercises with their sets pre-filled from last time. */
    suspend fun toWorkout(workout: WorkoutId, selection: List<ExerciseId>, asSuperset: Boolean) {
        val previous = history.latestSessions(before = clock.millis(), ids = selection.toSet())
        val added = exercises.infos(selection).map { factory.exercise(it, previous[it.id]?.sets.orEmpty()) }
        workouts.edit(workout) { it.addExercises(added, asSuperset && added.size > 1) }
    }

    suspend fun replaceInWorkout(workout: WorkoutId, slot: WorkoutExerciseId, replacement: ExerciseId) {
        val info = exercises.infos(listOf(replacement)).firstOrNull() ?: return
        workouts.edit(workout) { it.replaceExercise(slot, info) }
    }

    suspend fun toRoutine(routine: RoutineId, selection: List<ExerciseId>, asSuperset: Boolean) {
        val added = exercises.infos(selection).map { RoutineExercise(RoutineExerciseId(ids.newId()), it) }
        routines.edit(routine) { it.addExercises(added, asSuperset && added.size > 1) }
    }

    suspend fun replaceInRoutine(routine: RoutineId, slot: RoutineExerciseId, replacement: ExerciseId) {
        val info = exercises.infos(listOf(replacement)).firstOrNull() ?: return
        routines.edit(routine) { it.editExercise(slot) { old -> old.copy(exercise = info) } }
    }
}
