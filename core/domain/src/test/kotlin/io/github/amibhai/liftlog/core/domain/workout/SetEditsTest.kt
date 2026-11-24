package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.model.missingField
import org.junit.jupiter.api.Test

class SetEditsTest {

    private val bench = WorkoutExerciseId("bench")
    private val id1 = WorkoutSetId("1")

    @Test
    fun `completing a set logs its ghost values with the time`() {
        val done = workout(exercise("bench", set("1", target = values(80.0, 8)))).completeSet(id1, NOW).set("1")

        assertThat(done.isCompleted).isTrue()
        assertThat(done.entered).isEqualTo(values(80.0, 8))
        assertThat(done.completedAt).isEqualTo(NOW)
    }

    @Test
    fun `typed values win over ghost values`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8), entered = values(reps = 6))))

        assertThat(start.completeSet(id1, NOW).set("1").entered).isEqualTo(values(80.0, 6))
    }

    @Test
    fun `a new weight carries over to the following sets that expected the old one`() {
        val start = workout(
            exercise(
                "bench",
                set("1", target = values(80.0, 8), entered = values(82.5)),
                set("2", target = values(80.0, 8)),
                set("3", target = values(70.0, 12)),
                set("4", target = values(80.0, 8), entered = values(77.5)),
            ),
        )

        val after = start.completeSet(id1, NOW)

        assertThat(after.set("2").target).isEqualTo(values(82.5, 8))
        assertThat(after.set("3").target).isEqualTo(values(70.0, 12))
        assertThat(after.set("4").shown).isEqualTo(values(77.5, 8))
    }

    @Test
    fun `fewer reps than planned do not lower the next goal`() {
        val start = workout(
            exercise(
                "bench",
                set("1", target = values(80.0, 8), entered = values(reps = 6)),
                set("2", target = values(80.0, 8)),
            ),
        )

        assertThat(start.completeSet(id1, NOW).set("2").target).isEqualTo(values(80.0, 8))
    }

    @Test
    fun `the first logged set fills the empty goals after it`() {
        val start = workout(exercise("bench", set("1", entered = values(60.0, 10)), set("2"), set("3")))

        val after = start.completeSet(id1, NOW)

        assertThat(after.set("2").target).isEqualTo(values(60.0, 10))
        assertThat(after.set("3").target).isEqualTo(values(60.0, 10))
    }

    @Test
    fun `a warm-up never sets the goal of working sets`() {
        val start = workout(
            exercise(
                "bench",
                set("w", SetType.WARMUP, entered = values(40.0, 10)),
                set("w2", SetType.WARMUP),
                set("1"),
            ),
        )

        val after = start.completeSet(WorkoutSetId("w"), NOW)

        assertThat(after.set("w2").target).isEqualTo(values(40.0, 10))
        assertThat(after.set("1").target).isEqualTo(SetValues.EMPTY)
    }

    @Test
    fun `reopening a set keeps its values`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8), done = true)))

        val open = start.reopenSet(id1).set("1")

        assertThat(open.isCompleted).isFalse()
        assertThat(open.completedAt).isNull()
        assertThat(open.entered).isEqualTo(values(80.0, 8))
    }

    @Test
    fun `add set copies the last set`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8)), set("2", entered = values(85.0, 6))))

        val added = start.addSet(bench, WorkoutSetId("3")).exercise("bench").sets.last()

        assertThat(added.id).isEqualTo(WorkoutSetId("3"))
        assertThat(added.target).isEqualTo(values(85.0, 6))
        assertThat(added.entered).isEqualTo(SetValues.EMPTY)
        assertThat(added.isCompleted).isFalse()
    }

    @Test
    fun `a set added after a warm-up or failure set is a working set`() {
        for (type in listOf(SetType.WARMUP, SetType.FAILURE)) {
            val start = workout(exercise("bench", set("1", type, target = values(80.0, 8))))

            assertThat(start.addSet(bench, WorkoutSetId("2")).set("2").type).isEqualTo(SetType.WORKING)
        }
        val drop = workout(exercise("bench", set("1", SetType.DROP, target = values(60.0, 8))))
        assertThat(drop.addSet(bench, WorkoutSetId("2")).set("2").type).isEqualTo(SetType.DROP)
    }

    @Test
    fun `the first set of an empty exercise is empty`() {
        val added = workout(exercise("bench")).addSet(bench, id1).set("1")

        assertThat(added.type).isEqualTo(SetType.WORKING)
        assertThat(added.shown).isEqualTo(SetValues.EMPTY)
    }

    @Test
    fun `duplicating puts an open copy right below`() {
        val start = workout(
            exercise(
                "bench",
                set("1", SetType.DROP, target = values(80.0, 8), done = true),
                set("2", target = values(70.0, 8)),
            ),
        )

        val after = start.duplicateSet(id1, WorkoutSetId("copy")).exercise("bench").sets

        assertThat(after.map { it.id.value }).containsExactly("1", "copy", "2").inOrder()
        assertThat(after[1].type).isEqualTo(SetType.DROP)
        assertThat(after[1].target).isEqualTo(values(80.0, 8))
        assertThat(after[1].isCompleted).isFalse()
    }

    @Test
    fun `a removed set can be put back where it was`() {
        val start = workout(exercise("bench", set("1"), set("2", target = values(80.0, 8)), set("3")))

        val removed = start.findSet(WorkoutSetId("2"))!!
        val after = start.removeSet(WorkoutSetId("2"))

        assertThat(after.exercise("bench").sets.map { it.id.value }).containsExactly("1", "3").inOrder()
        assertThat(after.insertSet(removed.exerciseId, removed.index, removed.set)).isEqualTo(start)
        assertThat(start.findSet(WorkoutSetId("missing"))).isNull()
    }

    @Test
    fun `typing sets and clears one value`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8))))

        val typed = start.updateSet(id1) { it.copy(entered = it.entered.copy(reps = 5)) }
        assertThat(typed.set("1").shown).isEqualTo(values(80.0, 5))

        val cleared = typed.updateSet(id1) { it.copy(entered = it.entered.copy(reps = null)) }
        assertThat(cleared.set("1").shown).isEqualTo(values(80.0, 8))
    }

    @Test
    fun `a set can be rated and the rating taken back`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8), done = true)))

        val rated = start.rateSet(id1, EffortMode.RPE, 8.5)
        assertThat(rated.set("1").rpe).isEqualTo(8.5)
        assertThat(rated.rateSet(id1, EffortMode.RIR, 2.0).set("1").rir).isEqualTo(2)
        assertThat(rated.rateSet(id1, EffortMode.RPE, null).set("1").rpe).isNull()
    }

    @Test
    fun `effort scales go from easy to all out`() {
        assertThat(EffortMode.RPE.scale.first()).isEqualTo(6.0)
        assertThat(EffortMode.RPE.scale.last()).isEqualTo(10.0)
        assertThat(EffortMode.RPE.scale).contains(8.5)
        assertThat(EffortMode.RIR.scale).containsExactly(5.0, 4.0, 3.0, 2.0, 1.0, 0.0).inOrder()
        assertThat(EffortMode.OFF.scale).isEmpty()
    }

    @Test
    fun `a set can only be completed when the numbers its type needs are there`() {
        assertThat(ExerciseType.WEIGHT_REPS.missingField(SetValues.EMPTY)).isEqualTo(SetField.WEIGHT)
        assertThat(ExerciseType.WEIGHT_REPS.missingField(values(kg = 80.0))).isEqualTo(SetField.REPS)
        assertThat(ExerciseType.WEIGHT_REPS.missingField(values(80.0, 8))).isNull()
        assertThat(ExerciseType.BODYWEIGHT_ADDED.missingField(values(reps = 8))).isNull()
        assertThat(ExerciseType.DISTANCE_DURATION.missingField(SetValues.EMPTY)).isEqualTo(SetField.DISTANCE)
        assertThat(ExerciseType.DISTANCE_DURATION.missingField(SetValues(durationSeconds = 600))).isNull()
        assertThat(ExerciseType.DURATION.missingField(values(reps = 8))).isEqualTo(SetField.DURATION)
        assertThat(ExerciseType.WEIGHT_DISTANCE.missingField(values(kg = 40.0))).isEqualTo(SetField.DISTANCE)
    }
}
