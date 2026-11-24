package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.units.Weight
import org.junit.jupiter.api.Test

class WarmupEditsTest {

    private val warmups = listOf(set("w1", SetType.WARMUP, target = values(20.0, 10)))

    @Test
    fun `warm-ups come before the working sets`() {
        val workout = workout(exercise("bench", set("1", target = values(80.0, 8))))

        assertThat(sets(workout.withWarmups(BENCH, warmups))).containsExactly("w1", "1").inOrder()
    }

    @Test
    fun `open warm-ups are replaced and done ones are kept`() {
        val workout = workout(
            exercise(
                "bench",
                set("old1", SetType.WARMUP, target = values(20.0, 10), done = true),
                set("old2", SetType.WARMUP, target = values(40.0, 5)),
                set("1", target = values(80.0, 8)),
            ),
        )

        assertThat(sets(workout.withWarmups(BENCH, warmups))).containsExactly("old1", "w1", "1").inOrder()
    }

    @Test
    fun `the working weight is that of the first working set`() {
        val exercise = exercise(
            "bench",
            set("w", SetType.WARMUP, target = values(40.0, 5)),
            set("1", target = values(80.0, 8), entered = values(85.0)),
        )

        assertThat(exercise.workingWeight).isEqualTo(Weight(85.0))
        assertThat(exercise("bench", set("1", target = values(reps = 8))).workingWeight).isNull()
        assertThat(exercise("bench", set("1", target = values(0.0, 8))).workingWeight).isNull()
    }

    private fun sets(workout: Workout) = workout.exercise("bench").sets.map { it.id.value }

    private companion object {
        val BENCH = WorkoutExerciseId("bench")
    }
}
