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
            val text = fieldText(current, field, locale)
            val isWeight = field == SetField.WEIGHT
            SetFieldUiModel(
                field = field,
                text = if (isWeight && text.isNotEmpty()) demo.type.weightSign + text else text,
                unit = if (isWeight) unitLabel else null,
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

/** Position of [ref]'s row in the gallery list: the intro, then each exercise's header and sets. */
internal fun GalleryState.itemIndex(ref: FieldRef): Int {
    val introAndOwnHeader = 2
    return introAndOwnHeader + exercises.take(ref.exercise).sumOf { it.sets.size + 1 } + ref.set
}

internal fun GalleryState.fieldText(set: DemoSet, field: SetField, locale: Locale = Locale.getDefault()): String =
    when (field) {
        SetField.WEIGHT -> displayValue(set, field)?.let { LiftLogFormat.decimal(it, locale) }.orEmpty()
        SetField.REPS -> set.reps?.toString().orEmpty()
        SetField.DURATION -> set.seconds?.let { LiftLogFormat.duration(it) }.orEmpty()
        SetField.DISTANCE -> ""
    }

private fun GalleryState.previousText(type: ExerciseType, previous: DemoSet, locale: Locale): String {
    val weight = type.weightSign + fieldText(previous, SetField.WEIGHT, locale)
    return when (type) {
        ExerciseType.WEIGHT_REPS, ExerciseType.BODYWEIGHT_ADDED, ExerciseType.BODYWEIGHT_ASSISTED ->
            "$weight × ${previous.reps}"

        ExerciseType.DURATION -> fieldText(previous, SetField.DURATION, locale)

        ExerciseType.REPS_ONLY, ExerciseType.DISTANCE_DURATION, ExerciseType.WEIGHT_DISTANCE ->
            previous.reps?.toString().orEmpty()
    }
}

/** Weight added to bodyweight, or taken off it, carries a sign. */
private val ExerciseType.weightSign: String
    get() = when (weightMeaning) {
        WeightMeaning.ADDED -> "+"
        WeightMeaning.ASSISTANCE -> "−"
        WeightMeaning.LOAD, WeightMeaning.NONE -> ""
    }
