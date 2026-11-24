package io.github.amibhai.liftlog.feature.settings.gallery

import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.ui.component.SetFieldUiModel
import io.github.amibhai.liftlog.core.ui.component.SetRowUiModel
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import java.util.Locale

/** Builds the set row exactly as a workout screen will: display unit, ghost values, focus. */
internal fun GalleryState.rowModel(
    exercise: Int,
    set: Int,
    unitLabel: String,
    locale: Locale = Locale.getDefault(),
): SetRowUiModel {
    val demo = exercises[exercise]
    val current = demo.sets[set]
    val weightUnit = when (demo.type.weightMeaning) {
        WeightMeaning.ADDED -> "+$unitLabel"
        WeightMeaning.ASSISTANCE -> "−$unitLabel"
        WeightMeaning.LOAD, WeightMeaning.NONE -> unitLabel
    }
    val focused = editing?.takeIf { it.exercise == exercise && it.set == set }?.field
    val showSuggestion = current.isGhost && current.type == SetType.WORKING && demo.type == ExerciseType.WEIGHT_REPS
    return SetRowUiModel(
        id = current.id,
        type = current.type,
        workingSetNumber = if (current.type == SetType.WORKING) {
            demo.sets.take(set + 1).count { it.type == SetType.WORKING }
        } else {
            null
        },
        previous = demo.previous.getOrNull(set)?.let { previousText(demo.type, it, locale) },
        fields = demo.type.fields.map { field ->
            SetFieldUiModel(
                field = field,
                text = fieldText(current, field, locale),
                unit = if (field == SetField.WEIGHT) weightUnit else null,
                isGhost = current.isGhost,
                isFocused = focused == field,
            )
        },
        suggestion = if (showSuggestion) {
            "+" + LiftLogFormat.decimal(step(FieldRef(exercise, set, SetField.WEIGHT)), locale) + " " + unitLabel
        } else {
            null
        },
        isCompleted = current.isCompleted,
    )
}

internal fun GalleryState.fieldText(set: DemoSet, field: SetField, locale: Locale = Locale.getDefault()): String =
    when (field) {
        SetField.WEIGHT -> displayValue(set, field)?.let { LiftLogFormat.decimal(it, locale) }.orEmpty()
        SetField.REPS -> set.reps?.toString().orEmpty()
        SetField.DURATION -> set.seconds?.let { LiftLogFormat.duration(it) }.orEmpty()
        SetField.DISTANCE -> ""
    }

private fun GalleryState.previousText(type: ExerciseType, previous: DemoSet, locale: Locale): String {
    val weight = fieldText(previous, SetField.WEIGHT, locale)
    return when (type) {
        ExerciseType.WEIGHT_REPS -> "$weight × ${previous.reps}"

        ExerciseType.BODYWEIGHT_ADDED -> "+$weight × ${previous.reps}"

        ExerciseType.BODYWEIGHT_ASSISTED -> "−$weight × ${previous.reps}"

        ExerciseType.DURATION -> fieldText(previous, SetField.DURATION, locale)

        ExerciseType.REPS_ONLY, ExerciseType.DISTANCE_DURATION, ExerciseType.WEIGHT_DISTANCE ->
            previous.reps?.toString().orEmpty()
    }
}
