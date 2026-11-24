package io.github.amibhai.liftlog.feature.workout.logging

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.progression.Advice
import io.github.amibhai.liftlog.core.domain.progression.Suggestion
import io.github.amibhai.liftlog.core.domain.progression.SuggestionReason
import io.github.amibhai.liftlog.feature.workout.bench
import io.github.amibhai.liftlog.feature.workout.exercise
import io.github.amibhai.liftlog.feature.workout.row
import io.github.amibhai.liftlog.feature.workout.set
import io.github.amibhai.liftlog.feature.workout.values
import io.github.amibhai.liftlog.feature.workout.workout
import org.junit.Test

class WorkoutUiMapperTest {

    private val settings = UserSettings()

    @Test
    fun aPlannedSetShowsLastTimeAndItsSuggestionAsGhosts() {
        val previous = mapOf(bench.id to listOf(LoggedSet(SetType.WORKING, values(80.0, 8))))
        val state = WorkoutUiMapper(settings, Past(previous), WorkoutLocalState())
            .state(workout(exercise("e", bench, set("s", target = values(82.5, 8)))))

        val row = state.items.filterIsInstance<WorkoutItem.Set>().single().row
        assertThat(row.previous).isEqualTo("80 × 8")
        assertThat(row.fields.map { it.text }).containsExactly("82.5", "8").inOrder()
        assertThat(row.fields.all { it.isGhost }).isTrue()
    }

    @Test
    fun whatGoesUpIsMarkedUntilItIsTyped() {
        val slot = RoutineExerciseId("slot")
        val reason = SuggestionReason.HeavierAfter(8)
        val advice = Advice(
            basis = listOf(LoggedSet(SetType.WORKING, values(80.0, 8)), LoggedSet(SetType.WORKING, values(80.0, 7))),
            suggestion = Suggestion(listOf(values(82.5, 8), values(80.0, 8)), reason),
        )
        val exercise = exercise(
            "e",
            bench,
            set("1", target = values(82.5, 8)),
            set("2", target = values(80.0, 8)),
            set("3", target = values(82.5, 8), entered = values(85.0)),
        ).copy(routineExerciseId = slot)

        val state = WorkoutUiMapper(settings, Past(advice = mapOf(slot to advice)), WorkoutLocalState())
            .state(workout(exercise))

        val rows = state.items.filterIsInstance<WorkoutItem.Set>().map { it.row }
        assertThat(rows[0].fields.map { it.note }).containsExactly("+2.5 kg", null).inOrder()
        assertThat(rows[1].fields.map { it.note }).containsExactly(null, "+1").inOrder()
        assertThat(rows[2].fields.first().note).isNull()
        assertThat(state.items.filterIsInstance<WorkoutItem.Header>().single().reason).isEqualTo(reason)
    }

    @Test
    fun aTypedValueIsNoGhost() {
        val state = WorkoutUiMapper(settings, Past(), WorkoutLocalState())
            .state(workout(exercise("e", bench, set("s", target = values(80.0, 8), entered = values(kg = 85.0)))))

        val fields = state.items.filterIsInstance<WorkoutItem.Set>().single().row.fields
        assertThat(fields.map { it.isGhost }).containsExactly(false, true).inOrder()
    }

    @Test
    fun volumeAndSetsDoneCountOnlyCompletedSetsAndNoWarmUps() {
        val state = WorkoutUiMapper(settings, Past(), WorkoutLocalState()).state(
            workout(
                exercise(
                    "e",
                    bench,
                    set("w", target = values(40.0, 10), type = SetType.WARMUP, done = true),
                    set("1", target = values(100.0, 5), done = true),
                    set("2", target = values(100.0, 5)),
                ),
            ),
        )

        assertThat(state.volume).isEqualTo("500 kg")
        assertThat(state.completedSets).isEqualTo(2)
        assertThat(state.totalSets).isEqualTo(3)
    }

    @Test
    fun theEffortChipsFollowTheSetJustCompleted() {
        val local = WorkoutLocalState(effortFor = WorkoutSetId("1"))
        val state = WorkoutUiMapper(settings.copy(effortMode = EffortMode.RIR), Past(), local).state(
            workout(exercise("e", bench, set("1", target = values(100.0, 5), done = true), set("2"))),
        )

        val keys = state.items.map { it.key }
        assertThat(keys).containsExactly("header-e", "1", "effort-1", "2", "add-e").inOrder()
        assertThat(state.items.filterIsInstance<WorkoutItem.Effort>().single().mode).isEqualTo(EffortMode.RIR)
    }

    @Test
    fun theFieldBeingTypedShowsWhatWasTyped() {
        val workout = workout(exercise("e", bench, set("1", target = values(100.0, 5))))
        val editing = PadEditing.start(workout.exercises[0].sets[0], SetField.REPS, bench.type, settings.weightUnit)
        val state = WorkoutUiMapper(settings, Past(), WorkoutLocalState(editing = editing)).state(workout)

        val reps = state.items.filterIsInstance<WorkoutItem.Set>().single().row.fields[1]
        assertThat(reps.isFocused).isTrue()
        assertThat(state.pad?.field).isEqualTo(SetField.REPS)
        assertThat(state.pad?.isLastField).isTrue()
    }

    @Test
    fun supersetsAreLetteredInOrder() {
        val state = WorkoutUiMapper(settings, Past(), WorkoutLocalState()).state(
            workout(exercise("a", bench, set("1"), group = 7), exercise("b", row, set("2"), group = 7)),
        )

        assertThat(state.exercises.map { it.supersetLetter }).containsExactly('A', 'A')
    }
}
