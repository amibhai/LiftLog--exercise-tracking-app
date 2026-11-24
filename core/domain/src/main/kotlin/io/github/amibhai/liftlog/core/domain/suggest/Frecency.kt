package io.github.amibhai.liftlog.core.domain.suggest

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** How often and how recently an exercise was done: every use counts 1 and halves every 30 days (SPEC §4). */
object Frecency {
    private const val HORIZON_HALF_LIVES = 10
    private const val HALF = 0.5

    val HALF_LIFE: Duration = 30.days

    /** Uses older than this weigh less than 0.1 % and can be left out. */
    val HORIZON: Duration = HALF_LIFE * HORIZON_HALF_LIVES

    fun scores(usages: List<ExerciseUsage>, now: Long): Map<ExerciseId, Double> = usages
        .groupingBy { it.exerciseId }
        .fold(0.0) { sum, usage -> sum + weight(usage.at, now, HALF_LIFE) }

    internal fun weight(at: Long, now: Long, halfLife: Duration): Double {
        val age = (now - at).coerceAtLeast(0).milliseconds
        return HALF.pow(age / halfLife)
    }
}
