package io.github.amibhai.liftlog.core.domain.progress

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class PlateauTest {

    private val now = TimeUnit.DAYS.toMillis(1_000)

    @Test
    fun `four flat sessions over two weeks are a plateau`() {
        val plateau = find(21 to 100.0, 14 to 99.0, 7 to 100.2, 1 to 98.0)

        assertThat(plateau).isEqualTo(Plateau(bestAt = at(21), weeks = 3))
    }

    @Test
    fun `a decline is a plateau too`() {
        assertThat(find(28 to 100.0, 21 to 97.0, 10 to 95.0, 3 to 94.0)?.weeks).isEqualTo(4)
    }

    @Test
    fun `a new best within the last sessions is progress`() {
        assertThat(find(28 to 100.0, 21 to 99.0, 14 to 99.0, 7 to 101.0, 1 to 100.0)).isNull()
        assertThat(find(28 to 95.0, 21 to 96.0, 14 to 97.0, 7 to 98.0, 1 to 99.0)).isNull()
    }

    @Test
    fun `too few sessions or too short a time say nothing`() {
        assertThat(find(21 to 100.0, 7 to 99.0, 1 to 99.0)).isNull()
        assertThat(find(9 to 100.0, 6 to 99.0, 4 to 99.0, 1 to 99.0)).isNull()
        assertThat(plateau(emptyList(), now)).isNull()
    }

    @Test
    fun `a lift that is not trained any more has no plateau`() {
        assertThat(find(60 to 100.0, 50 to 99.0, 40 to 99.0, 30 to 99.0)).isNull()
    }

    @Test
    fun `after a long break the old best no longer counts`() {
        val sessions = arrayOf(90 to 120.0, 83 to 121.0, 21 to 100.0, 14 to 103.0, 7 to 106.0, 1 to 109.0)

        assertThat(find(*sessions)).isNull()
        assertThat(find(90 to 120.0, 22 to 100.0, 15 to 100.0, 8 to 100.0, 1 to 100.0)?.weeks).isEqualTo(3)
    }

    @Test
    fun `sessions without an estimate are left out`() {
        val withGaps = sessions(21 to 100.0, 14 to 99.0, 7 to 100.0, 1 to 98.0) +
            SessionProgress(WorkoutId("x"), at(2), bestE1rm = null)

        assertThat(plateau(withGaps, now)?.weeks).isEqualTo(3)
    }

    private fun find(vararg sessions: Pair<Int, Double>) = plateau(sessions(*sessions), now)

    private fun sessions(vararg sessions: Pair<Int, Double>) = sessions.map { (daysAgo, e1rm) ->
        SessionProgress(WorkoutId("$daysAgo"), at(daysAgo), bestE1rm = e1rm)
    }

    private fun at(daysAgo: Int) = now - TimeUnit.DAYS.toMillis(daysAgo.toLong())
}
