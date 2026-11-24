package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.progression.Advice

/** Creates workouts with their sets pre-filled from history (SPEC §5 start options). */
class WorkoutFactory(private val ids: IdGenerator) {

    fun empty(now: Long) = Workout(id = WorkoutId(ids.newId()), startedAt = now)

    /** The sets follow the [advice] for each exercise, and where there is none, the last time it was done. */
    fun fromRoutine(
        routine: Routine,
        history: Map<ExerciseId, List<LoggedSet>>,
        advice: Map<RoutineExerciseId, Advice>,
        now: Long,
    ): Workout = empty(now).copy(
        name = routine.name,
        routineId = routine.id,
        exercises = routine.exercises.map { slot ->
            val basis = advice[slot.id]?.basis.orEmpty().ifEmpty { history[slot.exercise.id].orEmpty() }
            val sets = SetPlanner.forRoutine(slot, basis, advice[slot.id]?.suggestion)
            newExercise(slot.exercise, sets).copy(
                supersetGroup = slot.supersetGroup,
                notes = slot.notes,
                restSeconds = slot.restSeconds,
                routineExerciseId = slot.id,
            )
        },
    )

    /** The same exercises again, with the latest numbers of each. */
    fun repeat(workout: Workout, history: Map<ExerciseId, List<LoggedSet>>, now: Long): Workout = empty(now).copy(
        name = workout.name,
        exercises = workout.exercises.map { old ->
            val latest = history[old.exercise.id] ?: old.sets.filter { it.isCompleted }.map { it.logged() }
            exercise(old.exercise, latest).copy(supersetGroup = old.supersetGroup, restSeconds = old.restSeconds)
        },
    )

    /** An exercise added by hand. */
    fun exercise(info: ExerciseInfo, previous: List<LoggedSet>): WorkoutExercise =
        newExercise(info, SetPlanner.forExercise(previous))

    private fun newExercise(info: ExerciseInfo, sets: List<PlannedSet>) = WorkoutExercise(
        id = WorkoutExerciseId(ids.newId()),
        exercise = info,
        sets = sets.map {
            WorkoutSet(id = WorkoutSetId(ids.newId()), type = it.type, target = it.target.forType(info.type))
        },
    )

    private fun WorkoutSet.logged() = LoggedSet(type, entered, rpe, rir)
}
