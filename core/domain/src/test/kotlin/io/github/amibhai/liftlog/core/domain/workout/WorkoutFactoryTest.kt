package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.progression.Advice
import io.github.amibhai.liftlog.core.domain.progression.Suggestion
import io.github.amibhai.liftlog.core.domain.progression.SuggestionReason
import org.junit.jupiter.api.Test

class WorkoutFactoryTest {

    private var next = 0
    private val factory = WorkoutFactory(IdGenerator { "id${next++}" })
    private val history: Map<ExerciseId, List<LoggedSet>> =
        mapOf(ExerciseId("bench") to listOf(logged(40.0, 10, SetType.WARMUP), logged(80.0, 8), logged(80.0, 7)))

    @Test
    fun `an empty workout starts now and is in progress`() {
        val workout = factory.empty(NOW)

        assertThat(workout.startedAt).isEqualTo(NOW)
        assertThat(workout.isInProgress).isTrue()
        assertThat(workout.exercises).isEmpty()
    }

    @Test
    fun `a routine fills in names, goals, supersets and rest`() {
        val routine = Routine(
            id = RoutineId("r"),
            name = "Push",
            exercises = listOf(
                planned("bench", targetSets = 2).copy(restSeconds = 180, notes = "Pause"),
                planned("row", targetSets = 3, group = 1).copy(repMin = 8, repMax = 12),
                planned("curl", targetSets = 3, group = 1),
            ),
        )

        val workout = factory.fromRoutine(routine, history, emptyMap(), NOW)

        assertThat(workout.name).isEqualTo("Push")
        assertThat(workout.routineId).isEqualTo(RoutineId("r"))
        assertThat(workout.exercises.map { it.supersetGroup }).containsExactly(null, 1, 1).inOrder()
        val bench = workout.exercises.first()
        assertThat(bench.restSeconds).isEqualTo(180)
        assertThat(bench.notes).isEqualTo("Pause")
        assertThat(bench.routineExerciseId).isEqualTo(RoutineExerciseId("r-bench"))
        assertThat(bench.sets.map { it.target })
            .containsExactly(values(40.0, 10), values(80.0, 8), values(80.0, 7)).inOrder()
        assertThat(workout.exercises[1].sets.map { it.target.reps }).containsExactly(8, 8, 8)
    }

    @Test
    fun `advice comes before the last time the exercise was done`() {
        val routine = Routine(RoutineId("r"), "Push", exercises = listOf(planned("bench", targetSets = 1)))
        val suggestion = Suggestion(listOf(values(62.5, 8)), SuggestionReason.HeavierAfter(12))
        val advice = Advice(basis = listOf(logged(60.0, 12)), suggestion = suggestion)

        val workout = factory.fromRoutine(routine, history, mapOf(RoutineExerciseId("r-bench") to advice), NOW)

        assertThat(workout.exercises.single().sets.map { it.target }).containsExactly(values(62.5, 8))
    }

    @Test
    fun `every row gets its own id`() {
        val workout = factory.fromRoutine(
            Routine(RoutineId("r"), "Push", exercises = listOf(planned("bench", 3), planned("row", 3))),
            history,
            emptyMap(),
            NOW,
        )

        val ids = listOf(workout.id.value) + workout.exercises.map { it.id.value } +
            workout.exercises.flatMap { it.sets }.map { it.id.value }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `repeating a workout takes its exercises and the latest numbers`() {
        val old = workout(
            exercise("bench", set("1", target = values(70.0, 8), done = true), group = 2),
            exercise("row", set("2", target = values(50.0, 10), done = true), group = 2),
        ).copy(name = "Evening lift", notes = "Felt good")

        val repeated = factory.repeat(old, history, NOW)

        assertThat(repeated.name).isEqualTo("Evening lift")
        assertThat(repeated.notes).isNull()
        assertThat(repeated.id).isNotEqualTo(old.id)
        assertThat(repeated.exercises.map { it.supersetGroup }).containsExactly(2, 2)
        assertThat(repeated.exercises[0].sets.map { it.target.weight?.kg }).containsExactly(40.0, 80.0, 80.0).inOrder()
        assertThat(repeated.exercises[1].sets.map { it.target }).containsExactly(values(50.0, 10))
        assertThat(repeated.exercises.flatMap { it.sets }.none { it.isCompleted }).isTrue()
    }

    private fun planned(name: String, targetSets: Int, group: Int? = null) = RoutineExercise(
        id = RoutineExerciseId("r-$name"),
        exercise = info(name),
        targetSets = targetSets,
        supersetGroup = group,
    )
}
