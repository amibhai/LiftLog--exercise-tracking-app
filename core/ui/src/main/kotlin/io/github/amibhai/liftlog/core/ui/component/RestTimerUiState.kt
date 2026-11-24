package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.runtime.Immutable

/**
 * The running rest timer (SPEC §6). Only the absolute end time is state; the remaining time is
 * recomputed from the clock, so it stays correct after backgrounding or process death.
 */
@Immutable
data class RestTimerUiState(val endsAtMillis: Long, val totalSeconds: Int)

private const val MILLIS_PER_SECOND = 1_000L

/** Seconds left, rounded up, so the display reaches 0:00 exactly when the rest is over. */
internal fun secondsLeft(state: RestTimerUiState, nowMillis: Long): Int {
    val remainingMillis = (state.endsAtMillis - nowMillis).coerceAtLeast(0)
    return ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
}

/** How long until the displayed (rounded-up) second changes, so the ticker wakes exactly then. */
internal fun millisUntilNextSecond(state: RestTimerUiState, nowMillis: Long): Long {
    val untilNextSecond = (state.endsAtMillis - nowMillis) % MILLIS_PER_SECOND
    return if (untilNextSecond <= 0L) MILLIS_PER_SECOND else untilNextSecond
}
