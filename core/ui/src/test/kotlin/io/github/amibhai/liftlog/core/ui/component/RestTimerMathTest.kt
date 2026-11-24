package io.github.amibhai.liftlog.core.ui.component

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import org.junit.Test

class RestTimerMathTest {

    private val state = RestTimerUiState(endsAtMillis = 100_000, totalSeconds = 90)

    @Test
    fun `remaining seconds round up so zero means the rest is over`() {
        assertThat(secondsLeft(state, nowMillis = 10_000)).isEqualTo(90)
        assertThat(secondsLeft(state, nowMillis = 10_001)).isEqualTo(90)
        assertThat(secondsLeft(state, nowMillis = 99_999)).isEqualTo(1)
        assertThat(secondsLeft(state, nowMillis = 100_000)).isEqualTo(0)
        assertThat(secondsLeft(state, nowMillis = 500_000)).isEqualTo(0)
    }

    @Test
    fun `the ticker wakes up exactly when the displayed second changes`() {
        assertThat(millisUntilNextSecond(state, nowMillis = 10_600)).isEqualTo(400)
        assertThat(millisUntilNextSecond(state, nowMillis = 10_000)).isEqualTo(1_000)
    }

    @Test
    fun `durations format as minutes and seconds`() {
        assertThat(LiftLogFormat.duration(45)).isEqualTo("0:45")
        assertThat(LiftLogFormat.duration(90)).isEqualTo("1:30")
        assertThat(LiftLogFormat.duration(3_723)).isEqualTo("1:02:03")
        assertThat(LiftLogFormat.duration(-5)).isEqualTo("0:00")
    }
}
