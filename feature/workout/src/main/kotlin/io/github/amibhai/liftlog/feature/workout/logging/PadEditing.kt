package io.github.amibhai.liftlog.feature.workout.logging

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.units.shown
import io.github.amibhai.liftlog.core.domain.workout.matchPrevious
import io.github.amibhai.liftlog.core.ui.component.DurationInputState
import io.github.amibhai.liftlog.core.ui.component.NumberInputState
import io.github.amibhai.liftlog.core.ui.component.PadInput
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import java.math.BigDecimal

/** The field the number pad is editing, with what has been typed. */
@Immutable
internal data class PadEditing(val setId: WorkoutSetId, val field: SetField, val input: PadInput) {

    val allowsDecimal: Boolean get() = this.field == SetField.WEIGHT || this.field == SetField.DISTANCE

    fun stepText(exercise: ExerciseInfo, settings: UserSettings): String {
        val step = step(exercise, settings)
        return if (field == SetField.DURATION) LiftLogFormat.duration(step.toInt()) else LiftLogFormat.decimal(step)
    }

    /** What −/+ add: the exercise's own weight step, or the default one (SPEC §2). */
    fun step(exercise: ExerciseInfo, settings: UserSettings): BigDecimal = when (field) {
        SetField.WEIGHT -> BigDecimal.valueOf(weightStep(exercise, settings))
        SetField.REPS -> BigDecimal.ONE
        SetField.DURATION -> DurationStep
        SetField.DISTANCE -> if (exercise.type == ExerciseType.WEIGHT_DISTANCE) CarryStep else CardioStep
    }

    private fun weightStep(exercise: ExerciseInfo, settings: UserSettings): Double {
        val own = when (settings.weightUnit) {
            WeightUnit.KG -> exercise.defaults.weightStepKg
            WeightUnit.LB -> exercise.defaults.weightStepLb
        }
        return own ?: settings.weightStep(exercise.equipment, settings.weightUnit)
    }

    companion object {
        private val DurationStep = BigDecimal(5)
        private val CarryStep = BigDecimal(5)
        private val CardioStep = BigDecimal("0.1")
        private val MaxValue = BigDecimal(9_999)

        fun start(set: WorkoutSet, field: SetField, type: ExerciseType, unit: WeightUnit): PadEditing {
            val value = set.shown.shown(field, type, unit)
            val input = when (field) {
                SetField.DURATION -> DurationInputState.start(value)
                SetField.REPS -> NumberInputState.start(value, maxDecimals = 0, maxValue = MaxValue)
                SetField.WEIGHT, SetField.DISTANCE -> NumberInputState.start(value, Weight.DISPLAY_DECIMALS, MaxValue)
            }
            return PadEditing(set.id, field, input)
        }
    }
}

/** What the field was last time, in the unit it is typed in. */
internal fun previousValue(
    exercise: WorkoutExercise,
    editing: PadEditing,
    previous: Map<ExerciseId, List<LoggedSet>>,
    unit: WeightUnit,
): BigDecimal? = previousSet(exercise, editing.setId, previous)
    ?.values?.shown(editing.field, exercise.exercise.type, unit)

/** The set at the same place last time. */
internal fun previousSet(
    exercise: WorkoutExercise,
    setId: WorkoutSetId,
    previous: Map<ExerciseId, List<LoggedSet>>,
): LoggedSet? {
    val matches = matchPrevious(exercise.sets.map { it.type }, previous[exercise.exercise.id].orEmpty())
    return matches.getOrNull(exercise.sets.indexOfFirst { it.id == setId })
}

/** Where "Next" goes: the next field of the set, then the first field of the next set. */
internal fun nextField(exercise: WorkoutExercise, editing: PadEditing): Pair<WorkoutSet, SetField>? {
    val fields = exercise.exercise.type.fields
    val setIndex = exercise.sets.indexOfFirst { it.id == editing.setId }
    val fieldIndex = fields.indexOf(editing.field)
    return when {
        setIndex < 0 -> null
        fieldIndex < fields.lastIndex -> exercise.sets[setIndex] to fields[fieldIndex + 1]
        else -> exercise.sets.getOrNull(setIndex + 1)?.let { it to fields.first() }
    }
}
