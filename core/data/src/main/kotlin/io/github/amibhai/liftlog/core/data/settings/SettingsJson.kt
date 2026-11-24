package io.github.amibhai.liftlog.core.data.settings

import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.model.ThemeMode
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WeightSteps
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import kotlinx.serialization.Serializable

/** The single source of default values: the domain model. */
private val DEFAULTS = UserSettings()

/**
 * On-disk shape of [UserSettings] (files/datastore/settings.json, included in Auto Backup).
 *
 * Every field has the domain default, and the serializer ignores unknown keys and coerces unknown
 * enum values, so settings written by older or newer builds always load.
 */
@Serializable
internal data class SettingsJson(
    val weightUnit: WeightUnit = DEFAULTS.weightUnit,
    val themeMode: ThemeMode = DEFAULTS.themeMode,
    val useDynamicColor: Boolean = DEFAULTS.useDynamicColor,
    val stepsKg: WeightStepsJson = DEFAULTS.stepsKg.toJson(),
    val stepsLb: WeightStepsJson = DEFAULTS.stepsLb.toJson(),
    val barWeightKg: Double = DEFAULTS.barWeightKg,
    val barWeightLb: Double = DEFAULTS.barWeightLb,
    val platesKg: List<PlateJson> = DEFAULTS.platesKg.map { it.toJson() },
    val platesLb: List<PlateJson> = DEFAULTS.platesLb.map { it.toJson() },
    val defaultRestSeconds: Int = DEFAULTS.defaultRestSeconds,
    val autoStartRestTimer: Boolean = DEFAULTS.autoStartRestTimer,
    val restTimerSound: Boolean = DEFAULTS.restTimerSound,
    val restTimerVibration: Boolean = DEFAULTS.restTimerVibration,
    val effortMode: EffortMode = DEFAULTS.effortMode,
    val keepScreenOn: Boolean = DEFAULTS.keepScreenOn,
)

@Serializable
internal data class WeightStepsJson(val standard: Double, val dumbbell: Double)

@Serializable
internal data class PlateJson(val weight: Double, val pairs: Int? = null)

private fun WeightSteps.toJson() = WeightStepsJson(standard, dumbbell)

private fun Plate.toJson() = PlateJson(weight, pairs)

internal fun SettingsJson.toDomain() = UserSettings(
    weightUnit = weightUnit,
    themeMode = themeMode,
    useDynamicColor = useDynamicColor,
    stepsKg = WeightSteps(stepsKg.standard, stepsKg.dumbbell),
    stepsLb = WeightSteps(stepsLb.standard, stepsLb.dumbbell),
    barWeightKg = barWeightKg,
    barWeightLb = barWeightLb,
    platesKg = platesKg.map { Plate(it.weight, it.pairs) },
    platesLb = platesLb.map { Plate(it.weight, it.pairs) },
    defaultRestSeconds = defaultRestSeconds,
    autoStartRestTimer = autoStartRestTimer,
    restTimerSound = restTimerSound,
    restTimerVibration = restTimerVibration,
    effortMode = effortMode,
    keepScreenOn = keepScreenOn,
)

internal fun UserSettings.toJson() = SettingsJson(
    weightUnit = weightUnit,
    themeMode = themeMode,
    useDynamicColor = useDynamicColor,
    stepsKg = stepsKg.toJson(),
    stepsLb = stepsLb.toJson(),
    barWeightKg = barWeightKg,
    barWeightLb = barWeightLb,
    platesKg = platesKg.map { it.toJson() },
    platesLb = platesLb.map { it.toJson() },
    defaultRestSeconds = defaultRestSeconds,
    autoStartRestTimer = autoStartRestTimer,
    restTimerSound = restTimerSound,
    restTimerVibration = restTimerVibration,
    effortMode = effortMode,
    keepScreenOn = keepScreenOn,
)
