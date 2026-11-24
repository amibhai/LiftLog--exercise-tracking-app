package io.github.amibhai.liftlog.feature.history

import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.time.partOfDay
import io.github.amibhai.liftlog.core.domain.workout.durationSeconds
import io.github.amibhai.liftlog.core.domain.workout.topSet
import io.github.amibhai.liftlog.core.domain.workout.totals
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Exercises listed on a history card; the rest are counted. */
private const val LISTED_EXERCISES = 4

/** Groups finished workouts (newest first) by month. */
internal fun historyItems(workouts: List<Workout>, settings: UserSettings, zone: ZoneId): List<HistoryItem> {
    val text = SetText(settings.weightUnit)
    var month: YearMonth? = null
    return buildList {
        for (workout in workouts) {
            val started = YearMonth.from(Instant.ofEpochMilli(workout.startedAt).atZone(zone))
            if (started != month) {
                month = started
                add(HistoryItem.Month(started, workout.startedAt))
            }
            add(entry(workout, settings, text, zone))
        }
    }
}

private fun entry(workout: Workout, settings: UserSettings, text: SetText, zone: ZoneId): HistoryItem.Entry {
    val totals = workout.totals()
    val lines = workout.exercises.map { exerciseLine(it, text) }
    return HistoryItem.Entry(
        id = workout.id,
        title = WorkoutTitle(workout.name, partOfDay(workout.startedAt, zone)),
        startedAt = workout.startedAt,
        durationSeconds = workout.durationSeconds(now = workout.startedAt).toInt(),
        volume = LiftLogFormat.volume(totals.volume, settings.weightUnit),
        sets = totals.completedSets,
        exercises = lines.take(LISTED_EXERCISES),
        moreExercises = (lines.size - LISTED_EXERCISES).coerceAtLeast(0),
    )
}

private fun exerciseLine(exercise: WorkoutExercise, text: SetText): ExerciseLine {
    val done = exercise.sets.filter(WorkoutSet::isCompleted)
    val type = exercise.exercise.type
    val best = topSet(done.map(WorkoutSet::logged), type)
    return ExerciseLine(exercise.exercise.name, done.size, best?.let { text.summary(type, it.values) })
}

internal fun WorkoutSet.logged() = LoggedSet(type, entered, rpe, rir)
