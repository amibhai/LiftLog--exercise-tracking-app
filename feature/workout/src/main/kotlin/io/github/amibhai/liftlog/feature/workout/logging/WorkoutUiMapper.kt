package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.time.partOfDay
import io.github.amibhai.liftlog.core.domain.workout.effort
import io.github.amibhai.liftlog.core.domain.workout.matchPrevious
import io.github.amibhai.liftlog.core.domain.workout.supersetLetters
import io.github.amibhai.liftlog.core.domain.workout.totals
import io.github.amibhai.liftlog.core.domain.workout.workingNumbers
import io.github.amibhai.liftlog.core.ui.component.RestTimerUiState
import io.github.amibhai.liftlog.core.ui.component.SetFieldUiModel
import io.github.amibhai.liftlog.core.ui.component.SetRowUiModel
import io.github.amibhai.liftlog.core.ui.component.effortText
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import java.time.ZoneId

/** What the screen adds to the stored workout. */
internal data class WorkoutLocalState(
    val editing: PadEditing? = null,
    /** The set whose effort chips are shown. */
    val effortFor: WorkoutSetId? = null,
    val focus: FocusTarget? = null,
    val message: WorkoutMessage? = null,
    val isFinished: Boolean = false,
)

/** Turns a stored workout into what the screen shows. Pure, so it is easy to test. */
internal class WorkoutUiMapper(
    private val settings: UserSettings,
    private val previous: Map<ExerciseId, List<LoggedSet>>,
    private val local: WorkoutLocalState,
) {
    private val text = SetText(settings.weightUnit)

    fun state(workout: Workout): WorkoutUiState.Ready {
        val totals = workout.totals()
        val letters = supersetLetters(workout.exercises) { it.supersetGroup }
        return WorkoutUiState.Ready(
            id = workout.id,
            title = title(workout),
            startedAt = workout.startedAt,
            endedAt = workout.endedAt,
            isInProgress = workout.isInProgress,
            notes = workout.notes,
            volume = LiftLogFormat.volume(totals.volume, settings.weightUnit),
            completedSets = totals.completedSets,
            totalSets = totals.totalSets,
            items = workout.exercises.flatMapIndexed { index, exercise -> items(exercise, letters[index]) },
            exercises = workout.exercises.mapIndexed { index, exercise ->
                ExerciseRef(exercise.id, exercise.exercise.name, letters[index])
            },
            restTimer = workout.restTimer?.let { RestTimerUiState(it.endsAt, it.totalSeconds) },
            pad = local.editing?.let { padModel(it, workout) },
            focus = local.focus,
            message = local.message,
            keepScreenOn = settings.keepScreenOn && workout.isInProgress,
            isFinished = local.isFinished,
        )
    }

    private fun items(exercise: WorkoutExercise, letter: Char?): List<WorkoutItem> = buildList {
        add(header(exercise, letter))
        val types = exercise.sets.map { it.type }
        val matches = matchPrevious(types, previous[exercise.exercise.id].orEmpty())
        val numbers = workingNumbers(exercise.sets)
        exercise.sets.forEachIndexed { index, set ->
            add(WorkoutItem.Set(row(exercise, set, numbers[index], matches[index])))
            if (set.id == local.effortFor && set.isCompleted) {
                add(WorkoutItem.Effort(set.id, settings.effortMode, set.effort(settings.effortMode)))
            }
        }
        add(WorkoutItem.AddSet(exercise.id))
    }

    private fun header(exercise: WorkoutExercise, letter: Char?) = WorkoutItem.Header(
        id = exercise.id,
        exerciseId = exercise.exercise.id,
        name = exercise.exercise.name,
        supersetLetter = letter,
        stickyNote = exercise.exercise.stickyNote,
        notes = exercise.notes,
        restSeconds = exercise.restSeconds,
        defaultRestSeconds = exercise.exercise.defaults.restSeconds ?: settings.defaultRestSeconds,
        columns = exercise.exercise.type.fields.map { ColumnUiModel(it, text.unitOf(exercise.exercise.type, it)) },
    )

    private fun row(exercise: WorkoutExercise, set: WorkoutSet, number: Int?, match: LoggedSet?): SetRowUiModel {
        val type = exercise.exercise.type
        return SetRowUiModel(
            id = set.id.value,
            type = set.type,
            workingSetNumber = number,
            previous = match?.let { text.compact(type, it.values) }?.ifEmpty { null },
            fields = type.fields.map { field(exercise, set, it) },
            isCompleted = set.isCompleted,
        )
    }

    private fun field(exercise: WorkoutExercise, set: WorkoutSet, field: SetField): SetFieldUiModel {
        val type = exercise.exercise.type
        val editing = local.editing?.takeIf { it.setId == set.id && it.field == field }
        val stored = text.field(type, set.shown, field)
        val effort = set.effort(settings.effortMode).takeIf { field == SetField.REPS }
        return SetFieldUiModel(
            field = field,
            // While it is edited, the row shows what was typed without waiting for the database.
            text = editing?.localizedDisplay() ?: stored,
            unit = text.unitOf(type, field),
            isGhost = !set.isCompleted && !set.entered.has(field),
            isFocused = editing != null,
            note = effort?.let { "${settings.effortMode.name} ${effortText(it)}" },
        )
    }

    private fun padModel(editing: PadEditing, workout: Workout): PadUiModel? {
        val exercise = workout.exercises.firstOrNull { it.sets.any { set -> set.id == editing.setId } } ?: return null
        return PadUiModel(
            field = editing.field,
            unit = text.unitOf(exercise.exercise.type, editing.field),
            display = editing.localizedDisplay(),
            step = editing.stepText(exercise.exercise, settings),
            allowsDecimal = editing.allowsDecimal,
            canUsePrevious = previousValue(exercise, editing, previous, settings.weightUnit) != null,
            isLastField = nextField(exercise, editing) == null,
        )
    }

    private fun title(workout: Workout) =
        WorkoutTitle(workout.name, partOfDay(workout.startedAt, ZoneId.systemDefault()))
}

private fun PadEditing.localizedDisplay(): String = input.display.replace('.', LiftLogFormat.decimalSeparator())
