package io.github.amibhai.liftlog.core.domain.model

import io.github.amibhai.liftlog.core.domain.progress.E1rmFormula
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Which effort scale the set row offers after completing a set (SPEC §5). */
enum class EffortMode { RPE, RIR, OFF }

/** A plate size in the inventory's unit; [pairs] is null when the supply is unlimited. */
data class Plate(val weight: Double, val pairs: Int? = null)

/** Default weight-step sizes in one unit system. */
data class WeightSteps(val standard: Double, val dumbbell: Double)

/**
 * App-wide settings (SPEC §12). Defaults come from SPEC §2. New fields must have defaults so
 * settings saved by older builds still load.
 */
data class UserSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColor: Boolean = true,
    val stepsKg: WeightSteps = WeightSteps(standard = DEFAULT_STEP_KG, dumbbell = DEFAULT_DUMBBELL_STEP_KG),
    val stepsLb: WeightSteps = WeightSteps(standard = DEFAULT_STEP_LB, dumbbell = DEFAULT_STEP_LB),
    val barWeightKg: Double = DEFAULT_BAR_KG,
    val barWeightLb: Double = DEFAULT_BAR_LB,
    val platesKg: List<Plate> = DEFAULT_PLATES_KG.map(::Plate),
    val platesLb: List<Plate> = DEFAULT_PLATES_LB.map(::Plate),
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val autoStartRestTimer: Boolean = true,
    val restTimerSound: Boolean = true,
    val restTimerVibration: Boolean = true,
    val effortMode: EffortMode = EffortMode.RPE,
    val keepScreenOn: Boolean = true,
    val e1rmFormula: E1rmFormula = E1rmFormula.EPLEY,
    /** How much of a set a secondary muscle gets in the weekly count (SPEC §8). */
    val secondaryMuscleSets: Double = DEFAULT_SECONDARY_SETS,
    val weeklySetTargetMin: Int = DEFAULT_TARGET_MIN,
    val weeklySetTargetMax: Int = DEFAULT_TARGET_MAX,
    /** Null follows the phone's language. */
    val firstDayOfWeek: DayOfWeek? = null,
) {
    val weeklySetTarget: IntRange get() = weeklySetTargetMin..weeklySetTargetMax

    fun weekStartsOn(locale: Locale = Locale.getDefault()): DayOfWeek =
        firstDayOfWeek ?: WeekFields.of(locale).firstDayOfWeek

    /** Default −/+ step for an exercise without its own override, in [unit]. */
    fun weightStep(equipment: Equipment, unit: WeightUnit): Double {
        val steps = if (unit == WeightUnit.KG) stepsKg else stepsLb
        return if (equipment == Equipment.DUMBBELL) steps.dumbbell else steps.standard
    }

    /** Default bar weight for barbell exercises in the unit system's usual bar; null without a bar. */
    fun barWeight(equipment: Equipment, unit: WeightUnit): Weight? {
        if (equipment != Equipment.BARBELL) return null
        val bar = if (unit == WeightUnit.KG) barWeightKg else barWeightLb
        return Weight.of(bar, unit)
    }

    fun plates(unit: WeightUnit): List<Plate> = if (unit == WeightUnit.KG) platesKg else platesLb

    private companion object {
        const val DEFAULT_STEP_KG = 2.5
        const val DEFAULT_DUMBBELL_STEP_KG = 2.0
        const val DEFAULT_STEP_LB = 5.0
        const val DEFAULT_BAR_KG = 20.0
        const val DEFAULT_BAR_LB = 45.0
        const val DEFAULT_REST_SECONDS = 120
        const val DEFAULT_SECONDARY_SETS = 0.5
        const val DEFAULT_TARGET_MIN = 10
        const val DEFAULT_TARGET_MAX = 20
        val DEFAULT_PLATES_KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        val DEFAULT_PLATES_LB = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    }
}
