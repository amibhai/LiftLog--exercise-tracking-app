package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.progression.Suggestion
import io.github.amibhai.liftlog.core.domain.progression.SuggestionReason
import org.junit.jupiter.api.Test

class SetPlannerTest {

    private val lastTime = listOf(
        logged(40.0, 10, SetType.WARMUP),
        logged(80.0, 8),
        logged(80.0, 8),
        logged(80.0, 7, SetType.FAILURE),
    )

    @Test
    fun `an exercise done before repeats the last session as ghost values`() {
        assertThat(SetPlanner.forExercise(lastTime).map { it.target })
            .containsExactly(values(40.0, 10), values(80.0, 8), values(80.0, 8), values(80.0, 7)).inOrder()
    }

    @Test
    fun `a set taken to failure last time is planned as a normal working set`() {
        assertThat(SetPlanner.forExercise(lastTime).map { it.type })
            .containsExactly(SetType.WARMUP, SetType.WORKING, SetType.WORKING, SetType.WORKING).inOrder()
    }

    @Test
    fun `a first-time exercise gets empty working sets`() {
        val plan = SetPlanner.forExercise(emptyList())

        assertThat(plan).hasSize(RoutineExercise.DEFAULT_SETS)
        assertThat(plan.all { it.type == SetType.WORKING && it.target.isEmpty }).isTrue()
    }

    @Test
    fun `a routine decides the number of working sets`() {
        val fewer = SetPlanner.forRoutine(routineExercise(targetSets = 2), lastTime)
        val more = SetPlanner.forRoutine(routineExercise(targetSets = 4), lastTime)

        assertThat(fewer.map { it.target })
            .containsExactly(values(40.0, 10), values(80.0, 8), values(80.0, 8)).inOrder()
        assertThat(more.map { it.target.reps }).containsExactly(10, 8, 8, 7, 7).inOrder()
    }

    @Test
    fun `drop sets stay behind the working set they followed`() {
        val previous = listOf(
            logged(80.0, 8),
            logged(60.0, 8, SetType.DROP),
            logged(80.0, 6),
            logged(60.0, 6, SetType.DROP),
        )

        val plan = SetPlanner.forRoutine(routineExercise(targetSets = 1), previous)

        assertThat(plan.map { it.type }).containsExactly(SetType.WORKING, SetType.DROP).inOrder()
    }

    @Test
    fun `the first time through a routine the bottom of the rep range is the goal`() {
        val plan = SetPlanner.forRoutine(routineExercise(targetSets = 3, repMin = 8, repMax = 12), emptyList())

        assertThat(plan.map { it.target }).containsExactly(values(reps = 8), values(reps = 8), values(reps = 8))
    }

    @Test
    fun `a routine without history or rep range has empty goals`() {
        assertThat(SetPlanner.forRoutine(routineExercise(targetSets = 1), emptyList()).single().target)
            .isEqualTo(SetValues.EMPTY)
    }

    @Test
    fun `a suggestion decides over the working sets and leaves warm-ups alone`() {
        val suggestion = Suggestion(listOf(values(82.5, 8), values(82.5, 8)), SuggestionReason.HeavierAfter(8))

        val plan = SetPlanner.forRoutine(routineExercise(targetSets = 3), lastTime, suggestion)

        assertThat(plan.map { it.target })
            .containsExactly(values(40.0, 10), values(82.5, 8), values(82.5, 8)).inOrder()
    }

    @Test
    fun `numbers that are not suggested stay as they were`() {
        val suggestion = Suggestion(listOf(values(reps = 5)), SuggestionReason.MoreReps)

        val plan = SetPlanner.forRoutine(routineExercise(targetSets = 1), listOf(logged(80.0, 8)), suggestion)

        assertThat(plan.single().target).isEqualTo(values(80.0, 5))
    }

    private fun routineExercise(targetSets: Int, repMin: Int? = null, repMax: Int? = null) = RoutineExercise(
        id = RoutineExerciseId("r"),
        exercise = info(),
        targetSets = targetSets,
        repMin = repMin,
        repMax = repMax,
    )
}
