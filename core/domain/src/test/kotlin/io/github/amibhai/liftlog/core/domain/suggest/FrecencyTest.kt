package io.github.amibhai.liftlog.core.domain.suggest

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

class FrecencyTest {

    private val now = 1_700_000_000_000L
    private val bench = ExerciseId("bench")
    private val curl = ExerciseId("curl")

    @Test
    fun `a use today scores one and halves every thirty days`() {
        assertThat(score(0)).isWithin(1e-9).of(1.0)
        assertThat(score(30)).isWithin(1e-9).of(0.5)
        assertThat(score(60)).isWithin(1e-9).of(0.25)
    }

    @Test
    fun `uses add up`() {
        val scores = Frecency.scores(listOf(used(bench, 0), used(bench, 30), used(curl, 0)), now)

        assertThat(scores.getValue(bench)).isWithin(1e-9).of(1.5)
        assertThat(scores.getValue(curl)).isWithin(1e-9).of(1.0)
    }

    @Test
    fun `a habit of last month beats a single use today`() {
        val habit = (25..45 step 5).map { used(bench, it) }

        val scores = Frecency.scores(habit + used(curl, 0), now)

        assertThat(scores.getValue(bench)).isGreaterThan(scores.getValue(curl))
    }

    @Test
    fun `a clock that went backwards does not inflate a score`() {
        assertThat(score(-10)).isWithin(1e-9).of(1.0)
    }

    private fun score(daysAgo: Int) = Frecency.scores(listOf(used(bench, daysAgo)), now).getValue(bench)

    private fun used(id: ExerciseId, daysAgo: Int) =
        ExerciseUsage(id, WorkoutId("w$daysAgo"), now - daysAgo.days.inWholeMilliseconds)
}
