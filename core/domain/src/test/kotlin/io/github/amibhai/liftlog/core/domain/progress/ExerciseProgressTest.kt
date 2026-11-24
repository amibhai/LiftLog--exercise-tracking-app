package io.github.amibhai.liftlog.core.domain.progress

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.units.Weight
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class ExerciseProgressTest {

    @Test
    fun `a session sums up its sets that count`() {
        val session = session(1, lift(40.0, 10, SetType.WARMUP), lift(80.0, 8), lift(85.0, 5))

        val progress = sessionProgress(ExerciseType.WEIGHT_REPS, listOf(session), E1rmFormula.EPLEY).single()

        assertThat(progress.topWeight).isEqualTo(85.0)
        assertThat(progress.volume).isEqualTo(80.0 * 8 + 85.0 * 5)
        assertThat(progress.bestReps).isEqualTo(8)
        assertThat(progress.totalReps).isEqualTo(13)
        assertThat(progress.bestE1rm).isWithin(0.01).of(80.0 * (1 + 8 / 30.0))
    }

    @Test
    fun `sessions come oldest first`() {
        val sessions = listOf(session(3, lift(1.0, 1)), session(1, lift(1.0, 1)), session(2, lift(1.0, 1)))

        assertThat(sessionProgress(ExerciseType.WEIGHT_REPS, sessions, E1rmFormula.EPLEY).map { it.at })
            .containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `a rep max counts everything done with at least that many reps`() {
        val sessions = listOf(session(1, lift(100.0, 5), lift(90.0, 8)), session(2, lift(95.0, 3)))

        val table = repMaxes(sessions)

        assertThat(table.first { it.reps == 1 }.weightKg).isEqualTo(100.0)
        assertThat(table.first { it.reps == 3 }.weightKg).isEqualTo(100.0)
        assertThat(table.first { it.reps == 6 }.weightKg).isEqualTo(90.0)
        assertThat(table.map { it.reps }).containsExactlyElementsIn(1..8)
        assertThat(table.first { it.reps == 6 }.doneReps).isEqualTo(8)
    }

    @Test
    fun `the chart metrics depend on the kind of exercise`() {
        assertThat(ExerciseType.WEIGHT_REPS.progressMetrics().first()).isEqualTo(ProgressMetric.E1RM)
        assertThat(ExerciseType.REPS_ONLY.progressMetrics()).containsExactly(ProgressMetric.REPS)
        assertThat(ExerciseType.WEIGHT_DISTANCE.progressMetrics())
            .containsExactly(ProgressMetric.TOP_WEIGHT, ProgressMetric.DISTANCE)
    }

    @Test
    fun `a range keeps the sessions of its last days`() {
        val now = TimeUnit.DAYS.toMillis(1_000)
        val days = listOf(0L, 20, 40, 100, 400).map { now - TimeUnit.DAYS.toMillis(it) }

        assertThat(days.within(ProgressRange.MONTH, now) { it }).hasSize(2)
        assertThat(days.within(ProgressRange.QUARTER, now) { it }).hasSize(3)
        assertThat(days.within(ProgressRange.ALL, now) { it }).hasSize(5)
    }

    @Test
    fun `an unknown value does not become a point of the chart`() {
        val progress = SessionProgress(WorkoutId("w"), at = 1)

        assertThat(ProgressMetric.entries.mapNotNull { progress.value(it) }).isEmpty()
    }

    private fun session(number: Int, vararg sets: LoggedSet) =
        ExerciseSession(WorkoutId("w$number"), at = number.toLong(), sets = sets.toList())

    private fun lift(kg: Double, reps: Int, type: SetType = SetType.WORKING) =
        LoggedSet(type, SetValues(weight = Weight(kg), reps = reps))
}
