package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import org.junit.jupiter.api.Test

class RoutineEditsTest {

    private val routine = Routine(RoutineId("r"), "Push", exercises = listOf(slot("a", 1), slot("b", 1), slot("c")))

    @Test
    fun `exercises are added at the end, optionally as a superset`() {
        assertThat(routine.addExercises(listOf(slot("d"), slot("e")), asSuperset = true).layout())
            .containsExactly("a" to 1, "b" to 1, "c" to null, "d" to 2, "e" to 2).inOrder()
    }

    @Test
    fun `removing and moving keep supersets intact`() {
        assertThat(routine.removeExercise(RoutineExerciseId("b")).layout())
            .containsExactly("a" to null, "c" to null).inOrder()
        assertThat(routine.moveExercise(2, 0).layout()).containsExactly("c" to null, "a" to 1, "b" to 1).inOrder()
    }

    @Test
    fun `supersets can be made and left`() {
        assertThat(routine.supersetWith(RoutineExerciseId("a"), setOf(RoutineExerciseId("c"))).layout())
            .containsExactly("a" to 1, "b" to 1, "c" to 1).inOrder()
        assertThat(routine.leaveSuperset(RoutineExerciseId("a")).layout())
            .containsExactly("a" to null, "b" to null, "c" to null).inOrder()
    }

    @Test
    fun `a removed exercise can be put back`() {
        val removed = routine.exercises[1]

        assertThat(routine.removeExercise(removed.id).insertExercise(1, removed, routine)).isEqualTo(routine)
    }

    private fun slot(id: String, group: Int? = null) =
        RoutineExercise(id = RoutineExerciseId(id), exercise = info(id), supersetGroup = group)

    private fun Routine.layout() = exercises.map { it.id.value to it.supersetGroup }
}
