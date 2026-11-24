package io.github.amibhai.liftlog.feature.workout.logging

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.progression.StageStep
import io.github.amibhai.liftlog.core.domain.progression.SuggestionReason
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.feature.workout.R

/** The last session of a 5/3/1 round. */
private const val DELOAD_WEEK = 4

/** Why an exercise is planned as it is, where the numbers do not say so themselves (SPEC §5). */
@Composable
internal fun suggestionText(reason: SuggestionReason, unit: WeightUnit): String? = when (reason) {
    is SuggestionReason.HeavierAfter ->
        pluralStringResource(R.plurals.workout_hint_heavier, reason.reps, reason.reps)

    is SuggestionReason.Retry -> stringResource(R.string.workout_hint_retry, reason.misses, reason.allowed)

    is SuggestionReason.Deload ->
        pluralStringResource(R.plurals.workout_hint_deload, reason.misses, reason.misses, reason.percent)

    is SuggestionReason.InStage -> stringResource(stageRes(reason.stage), reason.number, reason.count)

    is SuggestionReason.Week -> stringResource(
        if (reason.week == DELOAD_WEEK) R.string.workout_hint_week_deload else R.string.workout_hint_week,
        reason.week,
        SetText(unit).weight(Weight(reason.trainingMaxKg)),
    )

    SuggestionReason.MoreReps, SuggestionReason.TopOfRange -> null
}

private fun stageRes(step: StageStep): Int = when (step) {
    StageStep.FIRST -> R.string.workout_hint_stage
    StageStep.HEAVIER -> R.string.workout_hint_stage_heavier
    StageStep.AFTER_A_MISS -> R.string.workout_hint_stage_next
    StageStep.STARTED_OVER -> R.string.workout_hint_stage_again
}
