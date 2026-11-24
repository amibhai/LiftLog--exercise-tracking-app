package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.RestTimer
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId

private const val MILLIS_PER_SECOND = 1_000L

/** What follows a completed set (SPEC §5): the set to do next, and whether to rest before it. */
data class NextStep(val setId: WorkoutSetId?, val restFirst: Boolean)

/** Within a superset the next exercise follows without rest; the rest comes after the round. */
fun Workout.nextStepAfter(setId: WorkoutSetId): NextStep {
    val exercise = exercises.firstOrNull { it.sets.any { set -> set.id == setId } }
    val partner = exercise?.let { nextInRound(it) }
    val next = partner?.firstOpen() ?: exercise?.let { nextOpenSet(it, setId) }
    val isDropSet = next?.type == SetType.DROP && next in exercise?.sets.orEmpty()
    return NextStep(next?.id, restFirst = next != null && partner == null && !isDropSet)
}

/** The set to do next: the one that follows the last completed set, or the first one. Null when all are done. */
fun Workout.upNext(): Pair<WorkoutExercise, WorkoutSet>? {
    val last = exercises.flatMap { it.sets }.filter { it.isCompleted }.maxByOrNull { it.completedAt ?: 0 }
    val next = if (last == null) exercises.firstNotNullOfOrNull { it.firstOpen() }?.id else nextStepAfter(last.id).setId
    val exercise = exercises.firstOrNull { it.sets.any { set -> set.id == next } }
    return exercise?.let { it to it.sets.first { set -> set.id == next } }
}

/** Completes a set and starts, or ends, the rest timer accordingly. */
fun Workout.logSet(setId: WorkoutSetId, now: Long, settings: UserSettings): Workout {
    val logged = completeSet(setId, now)
    if (!settings.autoStartRestTimer) return logged
    val seconds = logged.exercises.firstOrNull { it.sets.any { set -> set.id == setId } }?.restSeconds(settings) ?: 0
    val rests = seconds > 0 && logged.nextStepAfter(setId).restFirst
    return logged.copy(restTimer = if (rests) restTimer(seconds, now) else null)
}

fun WorkoutExercise.restSeconds(settings: UserSettings): Int =
    restSeconds ?: exercise.defaults.restSeconds ?: settings.defaultRestSeconds

fun restTimer(seconds: Int, now: Long) = RestTimer(endsAt = now + seconds * MILLIS_PER_SECOND, totalSeconds = seconds)

/** −15 s / +15 s; null when no rest is left. An expired timer is extended from now. */
fun RestTimer.adjustedBy(seconds: Int, now: Long): RestTimer? {
    val end = maxOf(endsAt, now) + seconds * MILLIS_PER_SECOND
    return if (end <= now) null else RestTimer(end, (totalSeconds + seconds).coerceAtLeast(1))
}

/** Ends the workout with what was actually done: open sets and untouched exercises go. */
fun Workout.finish(now: Long): Workout {
    val done = exercises.map { it.copy(sets = it.sets.filter(WorkoutSet::isCompleted)) }.filter { it.sets.isNotEmpty() }
    return copy(
        exercises = workoutSupersets.normalize(done),
        endedAt = now,
        isInProgress = false,
        restTimer = null,
    )
}

/** The next superset member, after [exercise] and around, that has done fewer sets than it. */
private fun Workout.nextInRound(exercise: WorkoutExercise): WorkoutExercise? {
    val members = superset(exercise)
    val index = members.indexOf(exercise)
    val others = members.drop(index + 1) + members.take(index)
    return others.firstOrNull { it.firstOpen() != null && it.doneCount < exercise.doneCount }
}

private fun Workout.nextOpenSet(exercise: WorkoutExercise, setId: WorkoutSetId): WorkoutSet? {
    val members = superset(exercise)
    val inGroup = if (members.size == 1) {
        val after = exercise.sets.dropWhile { it.id != setId }.firstOrNull { !it.isCompleted }
        listOfNotNull(after ?: exercise.firstOpen())
    } else {
        members.mapNotNull { it.firstOpen() }
    }
    val end = exercises.indexOf(members.last()) + 1
    val elsewhere = exercises.drop(end) + exercises.take(end)
    return inGroup.firstOrNull() ?: elsewhere.firstNotNullOfOrNull { it.firstOpen() }
}

private fun Workout.superset(exercise: WorkoutExercise): List<WorkoutExercise> = if (exercise.supersetGroup == null) {
    listOf(exercise)
} else {
    exercises.filter { it.supersetGroup == exercise.supersetGroup }
}

private val WorkoutExercise.doneCount get() = sets.count { it.isCompleted }

private fun WorkoutExercise.firstOpen(): WorkoutSet? = sets.firstOrNull { !it.isCompleted }
