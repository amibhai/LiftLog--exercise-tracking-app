package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import org.junit.jupiter.api.Test

class ExerciseEditsTest {

    private val a = exercise("a")
    private val b = exercise("b")
    private val c = exercise("c")
    private val d = exercise("d")

    @Test
    fun `new exercises go to the end and can be added as a superset`() {
        val start = workout(exercise("a", group = 1), exercise("b", group = 1))

        assertThat(start.addExercises(listOf(c), asSuperset = true).layout())
            .containsExactly("a" to 1, "b" to 1, "c" to null).inOrder()
        assertThat(start.addExercises(listOf(c, d), asSuperset = true).layout())
            .containsExactly("a" to 1, "b" to 1, "c" to 2, "d" to 2).inOrder()
        assertThat(start.addExercises(listOf(c, d), asSuperset = false).layout())
            .containsExactly("a" to 1, "b" to 1, "c" to null, "d" to null).inOrder()
    }

    @Test
    fun `replacing an exercise keeps its sets but drops numbers the new type does not use`() {
        val start = workout(exercise("a", set("1", target = values(80.0, 8)), set("2", target = values(80.0, 8))))

        val after = start.replaceExercise(WorkoutExerciseId("a"), info("Plank", ExerciseType.DURATION))

        assertThat(after.exercise("a").exercise.name).isEqualTo("Plank")
        assertThat(after.exercise("a").sets).hasSize(2)
        assertThat(after.set("1").shown).isEqualTo(SetValues.EMPTY)
    }

    @Test
    fun `removing one of two superset members ends the superset`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c)

        assertThat(start.removeExercise(WorkoutExerciseId("a")).layout())
            .containsExactly("b" to null, "c" to null).inOrder()
    }

    @Test
    fun `a removed exercise can be put back with its superset`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c)

        val after = start.removeExercise(WorkoutExerciseId("b")).insertExercise(1, b.copy(supersetGroup = 1), start)

        assertThat(after.layout()).isEqualTo(start.layout())
    }

    @Test
    fun `moving keeps the other order`() {
        assertThat(workout(a, b, c).moveExercise(from = 0, to = 2).exercises.map { it.id.value })
            .containsExactly("b", "c", "a").inOrder()
    }

    @Test
    fun `an exercise moved out of its superset leaves it`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c, d)

        assertThat(start.moveExercise(from = 0, to = 3).layout())
            .containsExactly("b" to null, "c" to null, "d" to null, "a" to null).inOrder()
    }

    @Test
    fun `moving within a superset keeps it together`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c.copy(supersetGroup = 1))

        assertThat(start.moveExercise(from = 0, to = 2).layout())
            .containsExactly("b" to 1, "c" to 1, "a" to 1).inOrder()
    }

    @Test
    fun `a new order keeps supersets that stay together`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c, d)

        assertThat(start.reorderExercises(listOf("c", "b", "a", "d").map(::WorkoutExerciseId)).layout())
            .containsExactly("c" to null, "b" to 1, "a" to 1, "d" to null).inOrder()
        assertThat(start.reorderExercises(listOf("a", "c", "b", "d").map(::WorkoutExerciseId)).layout())
            .containsExactly("a" to null, "c" to null, "b" to null, "d" to null).inOrder()
    }

    @Test
    fun `a new order that forgets an exercise keeps it at the end`() {
        assertThat(workout(a, b, c).reorderExercises(listOf("c", "a").map(::WorkoutExerciseId)).layout())
            .containsExactly("c" to null, "a" to null, "b" to null).inOrder()
    }

    @Test
    fun `grouping brings the exercises together`() {
        assertThat(
            workout(a, b, c, d).supersetWith(WorkoutExerciseId("a"), setOf(WorkoutExerciseId("d"))).layout(),
        ).containsExactly("a" to 1, "d" to 1, "b" to null, "c" to null).inOrder()
    }

    @Test
    fun `grouping with a superset member joins its superset`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c)

        assertThat(start.supersetWith(WorkoutExerciseId("a"), setOf(WorkoutExerciseId("c"))).layout())
            .containsExactly("a" to 1, "b" to 1, "c" to 1).inOrder()
    }

    @Test
    fun `leaving a superset of three keeps the other two grouped`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c.copy(supersetGroup = 1))

        assertThat(start.leaveSuperset(WorkoutExerciseId("c")).layout())
            .containsExactly("a" to 1, "b" to 1, "c" to null).inOrder()
    }

    @Test
    fun `the middle of a superset leaves to the end of it`() {
        val start = workout(a.copy(supersetGroup = 1), b.copy(supersetGroup = 1), c.copy(supersetGroup = 1), d)

        assertThat(start.leaveSuperset(WorkoutExerciseId("b")).layout())
            .containsExactly("a" to 1, "c" to 1, "b" to null, "d" to null).inOrder()
    }
}
