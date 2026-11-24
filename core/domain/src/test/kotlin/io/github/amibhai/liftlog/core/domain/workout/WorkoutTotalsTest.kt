package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.units.Weight
import org.junit.jupiter.api.Test

class WorkoutTotalsTest {

    @Test
    fun `volume counts completed sets that are not warm-ups`() {
        val totals = workout(
            exercise(
                "bench",
                set("w", SetType.WARMUP, target = values(40.0, 10), done = true),
                set("1", target = values(80.0, 8), done = true),
                set("2", SetType.DROP, target = values(60.0, 10), done = true),
                set("3", target = values(80.0, 8)),
            ),
        ).totals()

        assertThat(totals.volume).isEqualTo(Weight(80.0 * 8 + 60.0 * 10))
        assertThat(totals.completedSets).isEqualTo(3)
        assertThat(totals.totalSets).isEqualTo(4)
    }

    @Test
    fun `added weight counts, assistance and unweighted exercises do not`() {
        val totals = workout(
            exercise("pullup", set("1", target = values(10.0, 6), done = true), type = ExerciseType.BODYWEIGHT_ADDED),
            exercise(
                "assisted",
                set("2", target = values(20.0, 8), done = true),
                type = ExerciseType.BODYWEIGHT_ASSISTED,
            ),
            exercise(
                "plank",
                set("3", target = SetValues(durationSeconds = 60), done = true),
                type = ExerciseType.DURATION,
            ),
        ).totals()

        assertThat(totals.volume).isEqualTo(Weight(60.0))
        assertThat(totals.completedSets).isEqualTo(3)
    }

    @Test
    fun `duration runs from start to end or until now`() {
        val running = workout(exercise("bench"))

        assertThat(running.durationSeconds(NOW)).isEqualTo(60)
        assertThat(running.copy(endedAt = NOW - 30_000).durationSeconds(NOW)).isEqualTo(30)
        assertThat(running.durationSeconds(NOW - 120_000)).isEqualTo(0)
    }

    @Test
    fun `working sets are numbered on their own`() {
        val sets = listOf(
            set("w", SetType.WARMUP),
            set("1"),
            set("d", SetType.DROP),
            set("2"),
            set("f", SetType.FAILURE),
        )

        assertThat(workingNumbers(sets)).containsExactly(null, 1, null, 2, null).inOrder()
    }

    @Test
    fun `supersets are lettered in the order they appear`() {
        val letters = supersetLetters(
            listOf(exercise("a"), exercise("b", group = 7), exercise("c", group = 7), exercise("d", group = 2)),
        ) { it.supersetGroup }

        assertThat(letters).containsExactly(null, 'A', 'A', 'B').inOrder()
    }

    @Test
    fun `the top set is the heaviest, then the one with most reps`() {
        val sets = listOf(logged(100.0, 1, SetType.WARMUP), logged(80.0, 8), logged(82.5, 6), logged(82.5, 7))

        assertThat(topSet(sets)).isEqualTo(logged(82.5, 7))
        assertThat(topSet(listOf(logged(40.0, 10, SetType.WARMUP)))).isEqualTo(logged(40.0, 10, SetType.WARMUP))
        assertThat(topSet(emptyList())).isNull()
    }
}
