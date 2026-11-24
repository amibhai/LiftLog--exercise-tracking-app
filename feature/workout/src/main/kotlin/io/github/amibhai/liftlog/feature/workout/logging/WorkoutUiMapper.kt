package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.plates.loadPlates
import io.github.amibhai.liftlog.core.domain.plates.warmupSets
import io.github.amibhai.liftlog.core.domain.progress.recordsBrokenBy
import io.github.amibhai.liftlog.core.domain.progression.Advice
import io.github.amibhai.liftlog.core.domain.time.partOfDay
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.workout.barWeight
import io.github.amibhai.liftlog.core.domain.workout.effort
import io.github.amibhai.liftlog.core.domain.workout.matchPrevious
import io.github.amibhai.liftlog.core.domain.workout.supersetLetters
import io.github.amibhai.liftlog.core.domain.workout.totals
import io.github.amibhai.liftlog.core.domain.workout.workingNumbers
import io.github.amibhai.liftlog.core.domain.workout.workingWeight
import io.github.amibhai.liftlog.core.ui.component.PlateBarUiModel
import io.github.amibhai.liftlog.core.ui.component.RestTimerUiState
import io.github.amibhai.liftlog.core.ui.component.SetFieldUiModel
import io.github.amibhai.liftlog.core.ui.component.SetRowUiModel
import io.github.amibhai.liftlog.core.ui.component.effortText
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import java.time.ZoneId
import kotlin.math.abs

/** Less than this is floating-point noise. */
private const val LEAST_CHANGE_KG = 0.005

/** What the screen adds to the stored workout. */
internal data class WorkoutLocalState(
    val editing: PadEditing? = null,
    /** The set whose effort chips are shown. */
    val effortFor: WorkoutSetId? = null,
    /** Sets just ticked or unticked, shown so at once, until the stored workout says the same. */
    val pendingDone: Map<WorkoutSetId, Boolean> = emptyMap(),
    val focus: FocusTarget? = null,
    val message: WorkoutMessage? = null,
    val isFinished: Boolean = false,
)

/** Turns a stored workout into what the screen shows. Pure, so it is easy to test. */
internal class WorkoutUiMapper(
    private val settings: UserSettings,
    private val past: Past,
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
        val matches = matchPrevious(types, past.previous[exercise.exercise.id].orEmpty())
        val basis = matchPrevious(types, exercise.advice?.basis.orEmpty())
        val numbers = workingNumbers(exercise.sets)
        exercise.sets.forEachIndexed { index, set ->
            add(WorkoutItem.Set(row(exercise, set, numbers[index], matches[index], basis[index])))
            if (set.id == local.effortFor && set.isDone) {
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
        canWarmUp = exercise.workingWeight?.let { warmupSets(exercise.exercise, it, settings) }.orEmpty().isNotEmpty(),
        reason = exercise.advice?.suggestion?.reason,
        unit = settings.weightUnit,
    )

    /** [basis] is the set of last time that a suggestion builds on. */
    private fun row(
        exercise: WorkoutExercise,
        set: WorkoutSet,
        number: Int?,
        match: LoggedSet?,
        basis: LoggedSet?,
    ): SetRowUiModel {
        val type = exercise.exercise.type
        val gains = if (exercise.advice?.suggestion == null || basis == null) emptyMap() else gains(type, set, basis)
        return SetRowUiModel(
            id = set.id.value,
            type = set.type,
            workingSetNumber = number,
            previous = match?.let { text.compact(type, it.values) }?.ifEmpty { null },
            fields = type.fields.map { field(exercise, set, it, gains[it]) },
            isCompleted = set.isDone,
            isRecord = set.isDone && set.breaksRecord(exercise),
        )
    }

    /** "+2.5 kg" or "+1", where the plan asks for more than last time (SPEC §5). */
    private fun gains(type: ExerciseType, set: WorkoutSet, basis: LoggedSet): Map<SetField, String> = buildMap {
        val weight = set.target.weight
        val change = if (weight == null) 0.0 else weight.kg - (basis.values.weight?.kg ?: 0.0)
        val isAssisted = type.weightMeaning == WeightMeaning.ASSISTANCE
        if (weight != null && (if (isAssisted) -change else change) > LEAST_CHANGE_KG) {
            put(SetField.WEIGHT, (if (isAssisted) "−" else "+") + text.weight(Weight(abs(change))))
        }
        val reps = (set.target.reps ?: 0) - (basis.values.reps ?: 0)
        if (reps > 0 && basis.values.reps != null) put(SetField.REPS, "+$reps")
    }

    private fun field(exercise: WorkoutExercise, set: WorkoutSet, field: SetField, gain: String?): SetFieldUiModel {
        val type = exercise.exercise.type
        val editing = local.editing?.takeIf { it.setId == set.id && it.field == field }
        val stored = text.field(type, set.shown, field)
        val effort = set.effort(settings.effortMode).takeIf { field == SetField.REPS }
        val isGhost = !set.isDone && !set.entered.has(field)
        return SetFieldUiModel(
            field = field,
            // While it is edited, the row shows what was typed without waiting for the database.
            text = editing?.localizedDisplay() ?: stored,
            unit = text.unitOf(type, field),
            isGhost = isGhost,
            isFocused = editing != null,
            note = effort?.let { "${settings.effortMode.name} ${effortText(it)}" } ?: gain?.takeIf { isGhost },
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
            canUsePrevious = previousValue(exercise, editing, past.previous, settings.weightUnit) != null,
            isLastField = nextField(exercise, editing) == null,
            plates = if (editing.field == SetField.WEIGHT) plates(editing, exercise) else null,
        )
    }

    /** The plates for the weight being typed (SPEC §5). */
    private fun plates(editing: PadEditing, exercise: WorkoutExercise): PlateBarUiModel? {
        val unit = settings.weightUnit
        val bar = exercise.exercise.barWeight(settings)
        val target = editing.input.value?.let { Weight.of(it, unit) }
        return if (bar == null || target == null) {
            null
        } else {
            PlateBarUiModel(loadPlates(target, bar, settings.plates(unit), unit), unit)
        }
    }

    private val WorkoutExercise.advice: Advice? get() = routineExerciseId?.let { past.advice[it] }

    private val WorkoutSet.isDone: Boolean get() = local.pendingDone[id] ?: isCompleted

    private fun WorkoutSet.breaksRecord(exercise: WorkoutExercise): Boolean {
        val logged = LoggedSet(type, shown, rpe, rir, id)
        val known = past.bests[exercise.exercise.id] ?: return false
        return recordsBrokenBy(exercise.exercise.type, logged, known, settings.e1rmFormula).isNotEmpty()
    }

    private fun title(workout: Workout) =
        WorkoutTitle(workout.name, partOfDay(workout.startedAt, ZoneId.systemDefault()))
}

private fun PadEditing.localizedDisplay(): String = input.display.replace('.', LiftLogFormat.decimalSeparator())

/** Forgets the ticks that the stored [workout] now shows, or whose sets are gone. */
internal fun WorkoutLocalState.confirmedBy(workout: Workout?): WorkoutLocalState {
    if (pendingDone.isEmpty()) return this
    val stored = workout?.exercises?.flatMap { it.sets }?.associateBy { it.id }.orEmpty()
    return copy(pendingDone = pendingDone.filter { (id, done) -> stored[id]?.let { it.isCompleted != done } ?: false })
}
