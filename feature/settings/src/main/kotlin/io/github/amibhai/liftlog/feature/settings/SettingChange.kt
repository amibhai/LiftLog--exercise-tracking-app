package io.github.amibhai.liftlog.feature.settings

import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.progress.E1rmFormula
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.time.DayOfWeek

/** A change to the logging settings (SPEC §12). Weights are in the unit that is chosen at the time. */
sealed interface SettingChange {
    data class WeightStep(val step: Double) : SettingChange

    data class DumbbellStep(val step: Double) : SettingChange

    data class BarWeight(val weight: Double) : SettingChange

    data class DefaultRest(val seconds: Int) : SettingChange

    data class AutoStartRest(val enabled: Boolean) : SettingChange

    data class TimerSound(val enabled: Boolean) : SettingChange

    data class TimerVibration(val enabled: Boolean) : SettingChange

    data class Effort(val mode: EffortMode) : SettingChange

    data class KeepScreenOn(val enabled: Boolean) : SettingChange

    data class WeeklySets(val least: Int, val most: Int) : SettingChange

    data class SecondaryMuscles(val share: Double) : SettingChange

    data class Formula(val formula: E1rmFormula) : SettingChange

    /** Null follows the phone's language. */
    data class FirstDay(val day: DayOfWeek?) : SettingChange
}

internal fun UserSettings.with(change: SettingChange): UserSettings {
    val isKg = weightUnit == WeightUnit.KG
    return when (change) {
        is SettingChange.WeightStep -> if (isKg) {
            copy(stepsKg = stepsKg.copy(standard = change.step))
        } else {
            copy(stepsLb = stepsLb.copy(standard = change.step))
        }

        is SettingChange.DumbbellStep -> if (isKg) {
            copy(stepsKg = stepsKg.copy(dumbbell = change.step))
        } else {
            copy(stepsLb = stepsLb.copy(dumbbell = change.step))
        }

        is SettingChange.BarWeight -> if (isKg) copy(barWeightKg = change.weight) else copy(barWeightLb = change.weight)

        is SettingChange.DefaultRest -> copy(defaultRestSeconds = change.seconds)

        is SettingChange.AutoStartRest -> copy(autoStartRestTimer = change.enabled)

        is SettingChange.TimerSound -> copy(restTimerSound = change.enabled)

        is SettingChange.TimerVibration -> copy(restTimerVibration = change.enabled)

        is SettingChange.Effort -> copy(effortMode = change.mode)

        is SettingChange.KeepScreenOn -> copy(keepScreenOn = change.enabled)

        is SettingChange.WeeklySets ->
            copy(weeklySetTargetMin = change.least, weeklySetTargetMax = change.most)

        is SettingChange.SecondaryMuscles -> copy(secondaryMuscleSets = change.share)

        is SettingChange.Formula -> copy(e1rmFormula = change.formula)

        is SettingChange.FirstDay -> copy(firstDayOfWeek = change.day)
    }
}

/** The weight settings as the chosen unit shows them. */
internal val UserSettings.weightStep: Double
    get() = if (weightUnit == WeightUnit.KG) stepsKg.standard else stepsLb.standard

internal val UserSettings.dumbbellStep: Double
    get() = if (weightUnit == WeightUnit.KG) stepsKg.dumbbell else stepsLb.dumbbell

internal val UserSettings.barWeight: Double
    get() = if (weightUnit == WeightUnit.KG) barWeightKg else barWeightLb
