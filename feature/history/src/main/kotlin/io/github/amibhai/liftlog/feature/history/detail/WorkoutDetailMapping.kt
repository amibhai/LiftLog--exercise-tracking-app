package io.github.amibhai.liftlog.feature.history.detail

import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.time.partOfDay
import io.github.amibhai.liftlog.core.domain.workout.differsFrom
import io.github.amibhai.liftlog.core.domain.workout.durationSeconds
import io.github.amibhai.liftlog.core.domain.workout.supersetLetters
import io.github.amibhai.liftlog.core.domain.workout.totals
import io.github.amibhai.liftlog.core.domain.workout.workingNumbers
import io.github.amibhai.liftlog.core.ui.component.effortText
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import java.time.ZoneId

internal fun detailState(
    workout: Workout,
    routine: Routine?,
    activeId: WorkoutId?,
    settings: UserSettings,
    local: DetailLocalState,
    justFinished: Boolean,
    zone: ZoneId,
): WorkoutDetailUiState.Ready {
    val text = SetText(settings.weightUnit)
    val totals = workout.totals()
    val letters = supersetLetters(workout.exercises) { it.supersetGroup }
    return WorkoutDetailUiState.Ready(
        id = workout.id,
        title = WorkoutTitle(workout.name, partOfDay(workout.startedAt, zone)),
        startedAt = workout.startedAt,
        durationSeconds = workout.durationSeconds(now = workout.startedAt).toInt(),
        volume = LiftLogFormat.volume(totals.volume, settings.weightUnit),
        sets = totals.completedSets,
        notes = workout.notes,
        exercises = workout.exercises.mapIndexed { index, exercise ->
            val done = exercise.sets.filter(WorkoutSet::isCompleted)
            val numbers = workingNumbers(done)
            DetailExercise(
                id = exercise.id,
                exerciseId = exercise.exercise.id,
                name = exercise.exercise.name,
                supersetLetter = letters[index],
                notes = exercise.notes,
                sets = done.mapIndexed { i, set ->
                    DetailSet(set.type, numbers[i], text.summary(exercise.exercise.type, set.entered), set.effortText())
                },
            )
        },
        routine = when {
            routine == null -> RoutineOffer.Save
            routine.differsFrom(workout) -> RoutineOffer.Update(routine.name)
            else -> RoutineOffer.None
        },
        justFinished = justFinished,
        activeWorkoutId = activeId?.takeIf { it != workout.id },
        routineDone = local.routineDone,
        openWorkout = local.openWorkout,
    )
}

/** Whatever was recorded, whichever scale is chosen now. */
private fun WorkoutSet.effortText(): String? = rpe?.let { "RPE ${effortText(it)}" } ?: rir?.let { "RIR $it" }
