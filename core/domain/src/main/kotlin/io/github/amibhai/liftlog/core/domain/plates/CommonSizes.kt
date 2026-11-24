package io.github.amibhai.liftlog.core.domain.plates

import io.github.amibhai.liftlog.core.domain.units.WeightUnit

/** What most gyms have, to choose from. */
val WeightUnit.commonBars: List<Double>
    get() = when (this) {
        WeightUnit.KG -> KgBars
        WeightUnit.LB -> LbBars
    }

val WeightUnit.commonPlates: List<Double>
    get() = when (this) {
        WeightUnit.KG -> KgPlates
        WeightUnit.LB -> LbPlates
    }

private val KgBars = listOf(20.0, 15.0, 10.0)
private val LbBars = listOf(45.0, 35.0, 25.0)
private val KgPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25, 1.0, 0.5)
private val LbPlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5, 1.25)
