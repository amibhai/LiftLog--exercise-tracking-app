package io.github.amibhai.liftlog.core.domain.plates

import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.units.Weight
import java.math.BigDecimal
import java.math.RoundingMode

/** The plates that one side of a bar can take. Weights are in hundredths of the unit. */
internal class PlateRack(inventory: List<Plate>) {

    private class Size(val plate: Plate) {
        val weight: Int = BigDecimal.valueOf(plate.weight)
            .movePointRight(Weight.DISPLAY_DECIMALS)
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()

        /** A pair puts one plate on each side. */
        val limit: Int = plate.pairs ?: Int.MAX_VALUE
    }

    private val sizes: List<Size> = inventory.map(::Size)
        .filter { it.weight > 0 && it.limit > 0 }
        .sortedByDescending { it.weight }

    /** Every weight is a multiple of this. */
    private val grain: Int = sizes.map { it.weight }.reduceOrNull(::greatestCommonDivisor) ?: 1

    val lightest: Int = sizes.lastOrNull()?.weight ?: 0

    /** Every weight up to [most] that one side can have, smallest first; 0 is always one. */
    fun reachable(most: Int): List<Int> {
        val lastPlate = lastPlates(most)
        return lastPlate.indices.filter { lastPlate[it] != UNREACHABLE }.map { it * grain }
    }

    /** The plates that make up [side], one of the [reachable] weights. */
    fun platesFor(side: Int): List<PlateCount> {
        val counts = heaviestFirst(side) ?: anyCombination(side)
        return sizes.zip(counts) { size, count -> PlateCount(size.plate.weight, count) }.filter { it.count > 0 }
    }

    /** What most people do: the heaviest plate that still fits, again and again. Null when that does not add up. */
    private fun heaviestFirst(side: Int): List<Int>? {
        var left = side
        val counts = sizes.map { size -> minOf(left / size.weight, size.limit).also { left -= it * size.weight } }
        return counts.takeIf { left == 0 }
    }

    private fun anyCombination(side: Int): List<Int> {
        val lastPlate = lastPlates(side)
        val counts = IntArray(sizes.size)
        var left = side / grain
        while (left > 0) {
            val index = lastPlate[left]
            counts[index]++
            left -= sizes[index].weight / grain
        }
        return counts.toList()
    }

    /** For every weight, in grains, the size of the plate that completes it; or that nothing does. */
    private fun lastPlates(most: Int): IntArray {
        val table = IntArray(most / grain + 1) { UNREACHABLE }
        table[0] = NOTHING
        sizes.forEachIndexed { index, size ->
            val weight = size.weight / grain
            val used = IntArray(table.size)
            for (total in weight until table.size) {
                val before = total - weight
                if (table[total] == UNREACHABLE && table[before] != UNREACHABLE && used[before] < size.limit) {
                    table[total] = index
                    used[total] = used[before] + 1
                }
            }
        }
        return table
    }

    private companion object {
        const val UNREACHABLE = -2
        const val NOTHING = -1
    }
}

private tailrec fun greatestCommonDivisor(a: Int, b: Int): Int = if (b == 0) a else greatestCommonDivisor(b, a % b)
