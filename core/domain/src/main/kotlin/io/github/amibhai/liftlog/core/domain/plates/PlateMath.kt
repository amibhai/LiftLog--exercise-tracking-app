package io.github.amibhai.liftlog.core.domain.plates

import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private const val SIDES = 2

/** Plates of one size on one side of the bar; [weight] is in the unit of the inventory. */
data class PlateCount(val weight: Double, val count: Int)

/** How to load a bar (SPEC §5). */
data class PlateLoad(
    val bar: Weight,
    /** For one side, heaviest first. */
    val perSide: List<PlateCount>,
    /** The bar with its plates. */
    val total: Weight,
    /** False when the target cannot be loaded exactly; [total] is then the closest below it, or the bar. */
    val isExact: Boolean,
)

/** The plates for [target] from the [inventory], whose weights are in [unit]. */
fun loadPlates(target: Weight, bar: Weight, inventory: List<Plate>, unit: WeightUnit): PlateLoad {
    val rack = PlateRack(inventory)
    val empty = bar.hundredths(unit)
    val goal = target.hundredths(unit)
    val side = rack.reachable((goal - empty).coerceAtLeast(0) / SIDES).last()
    return PlateLoad(
        bar = bar,
        perSide = rack.platesFor(side),
        total = weightOf(empty + side * SIDES, unit),
        isExact = empty + side * SIDES == goal,
    )
}

/** The loadable weight closest to [target]; the lighter one when two are equally close. */
fun nearestLoadable(target: Weight, bar: Weight, inventory: List<Plate>, unit: WeightUnit): Weight {
    val rack = PlateRack(inventory)
    val empty = bar.hundredths(unit)
    val room = (target.hundredths(unit) - empty).coerceAtLeast(0)
    val side = rack.reachable(room / SIDES + rack.lightest).minBy { abs(it * SIDES - room) }
    return weightOf(empty + side * SIDES, unit)
}

/** Weights are counted in hundredths of the unit, as precisely as they are shown. */
private fun Weight.hundredths(unit: WeightUnit): Int =
    displayValue(unit).movePointRight(Weight.DISPLAY_DECIMALS).setScale(0, RoundingMode.HALF_UP).toInt()

private fun weightOf(hundredths: Int, unit: WeightUnit): Weight =
    Weight.of(BigDecimal(hundredths).movePointLeft(Weight.DISPLAY_DECIMALS), unit)
