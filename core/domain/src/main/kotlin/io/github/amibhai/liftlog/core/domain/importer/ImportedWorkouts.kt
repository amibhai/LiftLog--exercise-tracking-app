package io.github.amibhai.liftlog.core.domain.importer

import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.model.missingField
import io.github.amibhai.liftlog.core.domain.units.DistanceUnit
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.workoutSupersets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

/** How long a set takes with its rest, for workouts without an end. */
private val SetMillis = TimeUnit.MINUTES.toMillis(3)

/** Distances from here on can only be meters. */
private const val METERS_FROM = 100.0

/** Other numbers cannot be meant as RPE. */
private val RpeRange = 1.0..10.0

/**
 * The log as workouts of this app. [exercises] says what each name stands for; names that are missing
 * there are left out. Ids follow from the file, so importing it again finds the same workouts.
 */
fun ImportedLog.toWorkouts(exercises: Map<String, ExerciseInfo>, unit: WeightUnit, zone: ZoneId): List<Workout> =
    workouts.mapNotNull { WorkoutBuilder(source, it, exercises, unit, zone).build() }

/** A custom exercise for a name the library does not know, with what its sets say about it. */
fun ImportedLog.draftFor(name: String): ExerciseDraft {
    val sets = setsOf(name)
    val bare = if (sets.any { it.weight != null }) Equipment.OTHER else Equipment.BODYWEIGHT
    return ExerciseDraft(name = name, type = typeOf(sets::any), equipment = equipmentIn(name) ?: bare)
}

/** The type that fits most of the sets under [name]; a few with weight do not make push-ups another exercise. */
fun ImportedLog.usualType(name: String): ExerciseType {
    val sets = setsOf(name)
    return typeOf { has -> sets.count(has) * 2 > sets.size }
}

private fun ImportedLog.setsOf(name: String): List<ImportedSet> =
    workouts.flatMap { it.exercises }.filter { it.name == name }.flatMap { it.sets }

private fun typeOf(have: ((ImportedSet) -> Boolean) -> Boolean): ExerciseType {
    val haveWeight = have { it.weight != null }
    val haveReps = have { it.reps != null }
    return when {
        have { it.distance != null } -> if (haveWeight) ExerciseType.WEIGHT_DISTANCE else ExerciseType.DISTANCE_DURATION
        haveReps -> if (haveWeight) ExerciseType.WEIGHT_REPS else ExerciseType.REPS_ONLY
        else -> ExerciseType.DURATION
    }
}

private class WorkoutBuilder(
    source: LogSource,
    private val imported: ImportedWorkout,
    private val exercises: Map<String, ExerciseInfo>,
    private val unit: WeightUnit,
    private val zone: ZoneId,
) {
    private val key = "$source/${imported.startedAt}/${imported.name}"
    private val startedAt = imported.startedAt.toMillis()
    private val setCount = imported.exercises.sumOf { it.sets.size }
    private val endedAt = imported.endedAt?.toMillis()?.takeIf { it > startedAt } ?: (startedAt + setCount * SetMillis)
    private val supersets = imported.exercises.mapNotNull { it.superset }.distinct()
    private var setsBuilt = 0

    fun build(): Workout? {
        val built = imported.exercises.mapIndexedNotNull { index, exercise ->
            exercises[exercise.name]?.let { build(exercise, it, "$key/$index") }
        }
        if (built.isEmpty()) return null
        return Workout(
            id = WorkoutId(stableId(key)),
            name = imported.name.ifBlank { null },
            startedAt = startedAt,
            endedAt = endedAt,
            notes = imported.notes,
            isInProgress = false,
            exercises = workoutSupersets.normalize(built),
        )
    }

    private fun build(exercise: ImportedExercise, info: ExerciseInfo, key: String): WorkoutExercise? {
        val sets = exercise.sets.mapIndexedNotNull { number, set -> build(set, info.type, "$key/$number") }
        if (sets.isEmpty()) return null
        return WorkoutExercise(
            id = WorkoutExerciseId(stableId(key)),
            exercise = info,
            supersetGroup = exercise.superset?.let { supersets.indexOf(it) + 1 },
            notes = exercise.notes,
            sets = sets,
        )
    }

    /** Null when the numbers an exercise of this type needs are not there. */
    private fun build(set: ImportedSet, type: ExerciseType, key: String): WorkoutSet? {
        setsBuilt++
        val values = values(set, type)
        if (type.missingField(values) != null) return null
        return WorkoutSet(
            id = WorkoutSetId(stableId(key)),
            type = set.type,
            entered = values,
            rpe = set.rpe?.takeIf { it in RpeRange },
            isCompleted = true,
            // Spread over the workout, because the files do not say when a set was done.
            completedAt = startedAt + (endedAt - startedAt) * setsBuilt / (setCount + 1),
        )
    }

    private fun values(set: ImportedSet, type: ExerciseType): SetValues {
        // A lift without a weight was done with the bare body or bar.
        val bare = Weight.ZERO.takeIf { type == ExerciseType.WEIGHT_REPS }
        return SetValues(
            weight = set.weight?.let { Weight.of(it, unit) } ?: bare,
            reps = set.reps,
            durationSeconds = set.seconds,
            distanceMeters = set.distance?.let { meters(it, set.distanceUnit) },
        ).forType(type)
    }

    private fun meters(distance: Double, unit: DistanceUnit?): Double {
        val guess = when {
            distance >= METERS_FROM -> DistanceUnit.METER
            this.unit == WeightUnit.KG -> DistanceUnit.KILOMETER
            else -> DistanceUnit.MILE
        }
        return (unit ?: guess).toMeters(distance.toBigDecimal())
    }

    private fun LocalDateTime.toMillis(): Long = atZone(zone).toInstant().toEpochMilli()

    private fun stableId(key: String): String =
        UUID.nameUUIDFromBytes("liftlog-import/$key".encodeToByteArray()).toString()
}
