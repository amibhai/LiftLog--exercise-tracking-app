package io.github.amibhai.liftlog.core.domain.progress

import java.util.concurrent.TimeUnit

/** Sessions without a new best before it is worth a word: with the best one, four in a row (SPEC §7). */
private const val SESSIONS_WITHOUT_PROGRESS = 3

/** Less than half a percent more is not progress but a matter of the day. */
private const val PROGRESS = 1.005

private val TwoWeeks = TimeUnit.DAYS.toMillis(14)
private val LongBreak = TimeUnit.DAYS.toMillis(28)
private val Week = TimeUnit.DAYS.toMillis(7)

/** A lift whose estimated one-rep max has not gone up since [bestAt]. */
data class Plateau(val bestAt: Long, val weeks: Int)

/**
 * Null while the lift moves, when it was not done often enough to tell, or when it is not trained any
 * more. After a long break the count starts again: getting back to old weights is no plateau.
 */
fun plateau(sessions: List<SessionProgress>, now: Long): Plateau? {
    val estimates = sessions.mapNotNull { session -> session.bestE1rm?.let { session.at to it } }.sortedBy { it.first }
    val recent = estimates.sinceLastBreak()
    val (bestIndex, best) = recent.withIndex().reduceOrNull { best, next ->
        if (next.value.second > best.value.second * PROGRESS) next else best
    } ?: return null
    val last = recent.last().first
    val isStuck = recent.lastIndex - bestIndex >= SESSIONS_WITHOUT_PROGRESS && last - best.first >= TwoWeeks
    val isTrained = now - last <= TwoWeeks
    return if (isStuck && isTrained) Plateau(best.first, ((now - best.first) / Week).toInt()) else null
}

private fun List<Pair<Long, Double>>.sinceLastBreak(): List<Pair<Long, Double>> {
    val lastBreak = zipWithNext().indexOfLast { (before, after) -> after.first - before.first >= LongBreak }
    return drop(lastBreak + 1)
}
