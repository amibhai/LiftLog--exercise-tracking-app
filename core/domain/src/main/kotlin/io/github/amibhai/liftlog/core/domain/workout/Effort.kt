package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId

private const val LOWEST_RPE_TIMES_TWO = 12
private const val HIGHEST_RPE_TIMES_TWO = 20
private const val MOST_RIR = 5

/** The values offered after a set, from easy to all out (SPEC §5). */
val EffortMode.scale: List<Double>
    get() = when (this) {
        EffortMode.RPE -> (LOWEST_RPE_TIMES_TWO..HIGHEST_RPE_TIMES_TWO).map { it / 2.0 }
        EffortMode.RIR -> (MOST_RIR downTo 0).map { it.toDouble() }
        EffortMode.OFF -> emptyList()
    }

fun WorkoutSet.effort(mode: EffortMode): Double? = when (mode) {
    EffortMode.RPE -> rpe
    EffortMode.RIR -> rir?.toDouble()
    EffortMode.OFF -> null
}

/** Null takes the rating back. */
fun Workout.rateSet(id: WorkoutSetId, mode: EffortMode, value: Double?): Workout = updateSet(id) { set ->
    when (mode) {
        EffortMode.RPE -> set.copy(rpe = value)
        EffortMode.RIR -> set.copy(rir = value?.toInt())
        EffortMode.OFF -> set
    }
}
