package io.github.amibhai.liftlog.core.domain.suggest

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import kotlin.time.Duration.Companion.days

/** Which exercises are usually done together with the current ones (SPEC §4). */
object GoesWith {
    /** Training habits change more slowly than single favorites. */
    private val HALF_LIFE = 90.days

    /**
     * For each exercise not in [current]: the share of past workouts with a current exercise that also had
     * it, summed over the current exercises and weighted by recency.
     */
    fun scores(current: Set<ExerciseId>, usages: List<ExerciseUsage>, now: Long): Map<ExerciseId, Double> {
        val sessions = usages.groupBy { it.workoutId }.values
        val total = mutableMapOf<ExerciseId, Double>()
        for (anchor in current) {
            val together = sessions.filter { session -> session.any { it.exerciseId == anchor } }
            val weights = together.map { Frecency.weight(it.first().at, now, HALF_LIFE) }
            val sum = weights.sum()
            together.forEachIndexed { index, session ->
                for (id in session.map { it.exerciseId }.toSet() - current) {
                    total.merge(id, weights[index] / sum, Double::plus)
                }
            }
        }
        return total
    }
}
