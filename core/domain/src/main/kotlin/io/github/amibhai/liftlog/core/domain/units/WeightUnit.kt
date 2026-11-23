package io.github.amibhai.liftlog.core.domain.units

/** The international avoirdupois pound, exactly. */
private const val KILOGRAMS_PER_POUND = 0.45359237

enum class WeightUnit(val kilogramsPerUnit: Double) {
    KG(kilogramsPerUnit = 1.0),
    LB(kilogramsPerUnit = KILOGRAMS_PER_POUND),
}
