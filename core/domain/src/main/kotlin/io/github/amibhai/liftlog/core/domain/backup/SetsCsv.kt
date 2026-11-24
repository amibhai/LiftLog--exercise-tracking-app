package io.github.amibhai.liftlog.core.domain.backup

import io.github.amibhai.liftlog.core.domain.csv.csvLine
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.supersetLetters
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

/** Every completed set as a row of a CSV file, for spreadsheets (SPEC §9). Oldest workout first. */
fun setsCsv(workouts: List<Workout>, unit: WeightUnit, zone: ZoneId): String = buildString {
    appendLine(csvLine(header(unit)))
    for (workout in workouts.sortedBy { it.startedAt }) {
        val letters = supersetLetters(workout.exercises) { it.supersetGroup }
        workout.exercises.forEachIndexed { index, exercise ->
            exercise.sets.filter { it.isCompleted }.forEachIndexed { number, set ->
                appendLine(csvLine(row(workout, exercise, letters[index], set, number + 1, unit, zone)))
            }
        }
    }
}

private fun header(unit: WeightUnit) = listOf(
    "Started",
    "Ended",
    "Workout",
    "Exercise",
    "Superset",
    "Set",
    "Type",
    "Weight (${unit.name.lowercase(Locale.ROOT)})",
    "Reps",
    "Distance (m)",
    "Time (s)",
    "RPE",
    "RIR",
    "Exercise notes",
    "Workout notes",
)

private fun row(
    workout: Workout,
    exercise: WorkoutExercise,
    superset: Char?,
    set: WorkoutSet,
    number: Int,
    unit: WeightUnit,
    zone: ZoneId,
): List<String> {
    val values = set.entered
    return listOf(
        time(workout.startedAt, zone),
        workout.endedAt?.let { time(it, zone) },
        workout.name,
        exercise.exercise.name,
        superset,
        number,
        set.type.name.lowercase(Locale.ROOT),
        values.weight?.displayText(unit),
        values.reps,
        values.distanceMeters?.let(::decimal),
        values.durationSeconds,
        set.rpe?.let(::decimal),
        set.rir,
        exercise.notes,
        workout.notes,
    ).map { it?.toString().orEmpty() }
}

private fun time(at: Long, zone: ZoneId): String = Time.format(Instant.ofEpochMilli(at).atZone(zone))

private fun decimal(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
