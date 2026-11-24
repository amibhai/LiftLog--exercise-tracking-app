package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetType
import org.junit.jupiter.api.Test

class RoutineFromWorkoutTest {

    private var next = 0
    private val ids = IdGenerator { "id${next++}" }

    private val done = workout(
        exercise(
            "bench",
            set("w", SetType.WARMUP, target = values(40.0, 10), done = true),
            set("1", target = values(80.0, 8), done = true),
            set("2", target = values(80.0, 7), done = true),
            set("d", SetType.DROP, target = values(60.0, 9), done = true),
            group = 1,
        ).copy(restSeconds = 150),
        exercise("row", set("r1", target = values(60.0, 12), done = true), group = 1),
    )

    @Test
    fun `a routine keeps the exercises, supersets, set counts and rest of the workout`() {
        val routine = routineFrom(done, "Push", ids)

        assertThat(routine.name).isEqualTo("Push")
        assertThat(routine.exercises.map { it.exercise.name }).containsExactly("bench", "row").inOrder()
        assertThat(routine.exercises.map { it.targetSets }).containsExactly(2, 1).inOrder()
        assertThat(routine.exercises.map { it.supersetGroup }).containsExactly(1, 1)
        assertThat(routine.exercises.first().restSeconds).isEqualTo(150)
    }

    @Test
    fun `the rep range is the usual range around the reps that were done`() {
        val ranges = routineFrom(done, "Push", ids).exercises.map { it.repMin to it.repMax }

        assertThat(ranges).containsExactly(6 to 8, 8 to 12).inOrder()
    }

    @Test
    fun `usual rep ranges cover strength to endurance`() {
        assertThat(RepRange.around(3)).isEqualTo(RepRange(3, 5))
        assertThat(RepRange.around(5)).isEqualTo(RepRange(3, 5))
        assertThat(RepRange.around(8)).isEqualTo(RepRange(6, 8))
        assertThat(RepRange.around(10)).isEqualTo(RepRange(8, 12))
        assertThat(RepRange.around(15)).isEqualTo(RepRange(12, 15))
        assertThat(RepRange.around(25)).isEqualTo(RepRange(15, 25))
        assertThat(RepRange.around(1)).isEqualTo(RepRange(1, 3))
    }

    @Test
    fun `updating a routine keeps what it already knew about an exercise`() {
        val bench = RoutineExercise(
            id = RoutineExerciseId("slot"),
            exercise = info("bench"),
            targetSets = 4,
            repMin = 5,
            repMax = 8,
            targetRpe = 8.0,
            notes = "Pause",
        )
        val old = Routine(
            RoutineId("r"),
            "Push",
            exercises = listOf(bench, bench.copy(RoutineExerciseId("gone"), info("dip"))),
        )
        val fromRoutine = done.copy(
            exercises = done.exercises.map {
                if (it.id.value == "bench") it.copy(routineExerciseId = RoutineExerciseId("slot")) else it
            },
        )

        val updated = old.updatedFrom(fromRoutine, ids)

        assertThat(updated.id).isEqualTo(old.id)
        assertThat(updated.exercises.map { it.exercise.name }).containsExactly("bench", "row").inOrder()
        val slot = updated.exercises.first()
        assertThat(slot.id).isEqualTo(RoutineExerciseId("slot"))
        assertThat(slot.targetSets).isEqualTo(2)
        assertThat(slot.repMin to slot.repMax).isEqualTo(5 to 8)
        assertThat(slot.targetRpe).isEqualTo(8.0)
        assertThat(slot.notes).isEqualTo("Pause")
    }

    @Test
    fun `a routine differs from a workout when exercises, order or set counts changed`() {
        val routine = routineFrom(done, "Push", ids)

        assertThat(routine.differsFrom(done)).isFalse()
        assertThat(routine.differsFrom(done.removeExercise(done.exercises.first().id))).isTrue()
        assertThat(routine.differsFrom(done.moveExercise(0, 1))).isTrue()
        assertThat(
            routine.copy(exercises = routine.exercises.map { it.copy(targetSets = 5) }).differsFrom(done),
        ).isTrue()
    }
}
