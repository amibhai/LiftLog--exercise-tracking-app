package io.github.amibhai.liftlog.core.domain.progress

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Muscle

/** The sets of one exercise in some time span, with the muscles they work. */
data class MuscleWork(
    val exerciseId: ExerciseId,
    val primary: List<Muscle>,
    val secondary: List<Muscle>,
    val sets: Int,
)

/** Sets per muscle (SPEC §8): a primary muscle gets one per set, a secondary one [secondarySets]. */
fun muscleSets(work: List<MuscleWork>, secondarySets: Double): Map<Muscle, Double> = buildMap {
    for (item in work) {
        val primary = item.primary.toSet()
        for (muscle in primary) merge(muscle, item.sets.toDouble(), Double::plus)
        if (secondarySets > 0) {
            for (muscle in item.secondary.toSet() - primary) merge(muscle, item.sets * secondarySets, Double::plus)
        }
    }
}

/** How a muscle's sets compare with the weekly target range. */
enum class MuscleLoad { NONE, BELOW, WITHIN, ABOVE }

fun muscleLoad(sets: Double, target: IntRange): MuscleLoad = when {
    sets <= 0.0 -> MuscleLoad.NONE
    sets < target.first -> MuscleLoad.BELOW
    sets <= target.last -> MuscleLoad.WITHIN
    else -> MuscleLoad.ABOVE
}
