package io.github.amibhai.liftlog.core.data.progress

import io.github.amibhai.liftlog.core.database.dao.NamedSetRow
import io.github.amibhai.liftlog.core.database.dao.PlannedSetRow
import io.github.amibhai.liftlog.core.database.dao.ProgressSetRow
import io.github.amibhai.liftlog.core.database.dao.RecordRow
import io.github.amibhai.liftlog.core.database.entity.PersonalRecordEntity
import io.github.amibhai.liftlog.core.domain.model.ExerciseHistory
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.PersonalRecord
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.RecordType
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.progression.PlannedSession
import io.github.amibhai.liftlog.core.domain.progression.SessionPlan
import io.github.amibhai.liftlog.core.domain.units.Weight

/** Rows in the order of the database: by time, then by place in the workout. */
internal fun List<ProgressSetRow>.toSessions(): List<ExerciseSession> = groupBy { it.workoutId }.values.map { rows ->
    ExerciseSession(WorkoutId(rows.first().workoutId), rows.first().at, rows.map { it.toLoggedSet() })
}

internal fun List<NamedSetRow>.toHistories(): List<ExerciseHistory> = groupBy { it.set.exerciseId }.values.map { rows ->
    val first = rows.first()
    ExerciseHistory(
        id = ExerciseId(first.set.exerciseId),
        name = first.exerciseName,
        type = first.exerciseType,
        sessions = rows.map { it.set }.toSessions(),
    )
}

/** One session per exercise of a workout, so that two slots with the same exercise stay apart. */
internal fun List<PlannedSetRow>.toPlannedSessions(): List<PlannedSession> =
    groupBy { it.workoutExerciseId }.values.map { rows ->
        val first = rows.first()
        PlannedSession(
            at = first.set.at,
            sets = rows.map { it.set.toLoggedSet() },
            slot = first.routineExerciseId?.let(::RoutineExerciseId),
            plan = first.progressionRule?.let { SessionPlan(first.repMin, first.repMax, it) },
        )
    }

private fun ProgressSetRow.toLoggedSet() = LoggedSet(
    type = type,
    values = SetValues(weightKg?.let(::Weight), reps, durationSeconds, distanceMeters),
    rpe = rpe,
    rir = rir,
    id = WorkoutSetId(setId),
)

internal fun PersonalRecord.toEntity(id: String, now: Long) = PersonalRecordEntity(
    id = id,
    exerciseId = exerciseId.value,
    recordType = type.name,
    value = value,
    weightKg = weightKg,
    reps = reps,
    workoutId = workoutId.value,
    workoutSetId = setId?.value,
    achievedAt = achievedAt,
    createdAt = now,
    updatedAt = now,
    deletedAt = null,
)

/** Null for a kind of record this version does not know. */
internal fun RecordRow.toDomain(): RecordEntry? =
    RecordType.entries.firstOrNull { it.name == record.recordType }?.let { type ->
        RecordEntry(
            record = PersonalRecord(
                exerciseId = ExerciseId(record.exerciseId),
                type = type,
                value = record.value,
                weightKg = record.weightKg,
                reps = record.reps,
                workoutId = WorkoutId(record.workoutId),
                setId = record.workoutSetId?.let(::WorkoutSetId),
                achievedAt = record.achievedAt,
            ),
            exerciseName = exerciseName,
            exerciseType = exerciseType,
        )
    }
