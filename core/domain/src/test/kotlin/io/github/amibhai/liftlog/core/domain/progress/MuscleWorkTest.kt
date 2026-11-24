package io.github.amibhai.liftlog.core.domain.progress

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Muscle
import org.junit.jupiter.api.Test

class MuscleWorkTest {

    @Test
    fun `a primary muscle gets a whole set and a secondary muscle half`() {
        val bench = MuscleWork(ExerciseId("bench"), listOf(Muscle.CHEST), listOf(Muscle.TRICEPS, Muscle.FRONT_DELTS), 4)
        val pushdown = MuscleWork(ExerciseId("pushdown"), listOf(Muscle.TRICEPS), emptyList(), 3)

        val sets = muscleSets(listOf(bench, pushdown), secondarySets = 0.5)

        assertThat(sets[Muscle.CHEST]).isEqualTo(4.0)
        assertThat(sets[Muscle.TRICEPS]).isEqualTo(5.0)
        assertThat(sets[Muscle.FRONT_DELTS]).isEqualTo(2.0)
        assertThat(sets).doesNotContainKey(Muscle.QUADS)
    }

    @Test
    fun `a muscle that is both primary and secondary counts once`() {
        val row = MuscleWork(ExerciseId("row"), listOf(Muscle.LATS), listOf(Muscle.LATS, Muscle.BICEPS), 2)

        assertThat(muscleSets(listOf(row), 0.5)[Muscle.LATS]).isEqualTo(2.0)
    }

    @Test
    fun `secondary muscles can be left out`() {
        val bench = MuscleWork(ExerciseId("bench"), listOf(Muscle.CHEST), listOf(Muscle.TRICEPS), 4)

        assertThat(muscleSets(listOf(bench), secondarySets = 0.0)).doesNotContainKey(Muscle.TRICEPS)
    }

    @Test
    fun `sets are compared with the target range`() {
        val target = 10..20

        assertThat(muscleLoad(0.0, target)).isEqualTo(MuscleLoad.NONE)
        assertThat(muscleLoad(9.5, target)).isEqualTo(MuscleLoad.BELOW)
        assertThat(muscleLoad(10.0, target)).isEqualTo(MuscleLoad.WITHIN)
        assertThat(muscleLoad(20.0, target)).isEqualTo(MuscleLoad.WITHIN)
        assertThat(muscleLoad(20.5, target)).isEqualTo(MuscleLoad.ABOVE)
    }
}
