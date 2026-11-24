package io.github.amibhai.liftlog.feature.settings.gallery

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.NumberPadKey
import io.github.amibhai.liftlog.core.ui.component.SetRowAction
import org.junit.Test
import java.util.Locale

/** The gallery wires the components the way the workout screen will, so its rules are tested. */
class GalleryStateTest {

    private val benchSet1 = FieldRef(exercise = 0, set = 1, field = SetField.WEIGHT)

    @Test
    fun `typing 45 in pounds displays back exactly 45 and stores kilograms`() {
        val state = GalleryState().withUnit(WeightUnit.LB)
            .startEditing(benchSet1)
            .press(NumberPadKey.Digit(4))
            .press(NumberPadKey.Digit(5))

        assertThat(state.fieldText(state.set(benchSet1), SetField.WEIGHT, Locale.US)).isEqualTo("45")
        assertThat(state.set(benchSet1).weightKg).isWithin(1e-9).of(20.41165665)
    }

    @Test
    fun `steps use the exercise step in the display unit`() {
        val kg = GalleryState().startEditing(benchSet1).press(NumberPadKey.StepUp)
        val lb = GalleryState().withUnit(WeightUnit.LB).startEditing(benchSet1).press(NumberPadKey.Digit(4))
            .press(NumberPadKey.Digit(5)).press(NumberPadKey.StepUp)

        assertThat(kg.fieldText(kg.set(benchSet1), SetField.WEIGHT, Locale.US)).isEqualTo("82.5")
        assertThat(lb.fieldText(lb.set(benchSet1), SetField.WEIGHT, Locale.US)).isEqualTo("50")
    }

    @Test
    fun `same as previous copies last time's value`() {
        val state = GalleryState().startEditing(benchSet1).press(NumberPadKey.SameAsPrevious)

        assertThat(state.set(benchSet1).weightKg).isEqualTo(77.5)
    }

    @Test
    fun `next walks the fields and then the sets and closes after the last one`() {
        val reps = GalleryState().startEditing(benchSet1).press(NumberPadKey.Next)
        assertThat(reps.editing).isEqualTo(benchSet1.copy(field = SetField.REPS))

        val nextSet = reps.press(NumberPadKey.Next)
        assertThat(nextSet.editing).isEqualTo(FieldRef(exercise = 0, set = 2, field = SetField.WEIGHT))

        val last = GalleryState().startEditing(FieldRef(exercise = 2, set = 1, field = SetField.DURATION))
        assertThat(last.press(NumberPadKey.Next).editing).isNull()
    }

    @Test
    fun `editing a ghost value turns it into a real value`() {
        val state = GalleryState().startEditing(benchSet1).press(NumberPadKey.Digit(9))

        assertThat(state.set(benchSet1).isGhost).isFalse()
    }

    @Test
    fun `completing a set starts the rest timer from an absolute end time`() {
        val now = 1_000_000L
        val state = GalleryState().handle(
            SetRowAction.ToggleComplete("bench-1"),
            exercise = 0,
            set = 1,
            nowMillis = now,
        )

        assertThat(state.set(benchSet1).isCompleted).isTrue()
        assertThat(state.restTimer!!.endsAtMillis).isEqualTo(now + GalleryState.REST_SECONDS * 1_000L)
        assertThat(
            state.adjustRest(15).restTimer!!.endsAtMillis,
        ).isEqualTo(now + (GalleryState.REST_SECONDS + 15) * 1_000L)
        assertThat(state.skipRest().restTimer).isNull()
    }

    @Test
    fun `tapping the badge cycles the set type`() {
        val state = GalleryState().handle(SetRowAction.CycleType("bench-1"), exercise = 0, set = 1, nowMillis = 0)

        assertThat(state.set(benchSet1).type).isEqualTo(SetType.WARMUP)
    }

    @Test
    fun `duplicating a set twice keeps every id unique`() {
        val duplicate = SetRowAction.Duplicate("bench-1")
        val state = GalleryState()
            .handle(duplicate, exercise = 0, set = 1, nowMillis = 0)
            .handle(duplicate, exercise = 0, set = 1, nowMillis = 0)

        val ids = state.exercises.first().sets.map { it.id }
        assertThat(ids).hasSize(6)
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `adding or removing a set closes the number pad so it cannot edit another row`() {
        val editing = GalleryState().startEditing(benchSet1)

        val duplicated = editing.handle(SetRowAction.Duplicate("bench-w"), exercise = 0, set = 0, nowMillis = 0)
        val deleted = editing.handle(SetRowAction.Delete("bench-w"), exercise = 0, set = 0, nowMillis = 0)

        assertThat(duplicated.editing).isNull()
        assertThat(deleted.editing).isNull()
    }

    @Test
    fun `weight added to bodyweight carries a plus sign`() {
        val row = GalleryState().rowModel(exercise = 1, set = 0, unitLabel = "kg", locale = Locale.US)

        assertThat(row.fields.first().text).isEqualTo("+10")
        assertThat(row.fields.first().unit).isEqualTo("kg")
        assertThat(row.previous).isEqualTo("+7.5 × 6")
    }

    @Test
    fun `a set's list position counts the intro and the exercise headers`() {
        val state = GalleryState()

        assertThat(state.itemIndex(benchSet1)).isEqualTo(3)
        assertThat(state.itemIndex(FieldRef(exercise = 1, set = 0, field = SetField.REPS))).isEqualTo(7)
    }

    @Test
    fun `working sets are numbered and warm-ups are not`() {
        val state = GalleryState()

        assertThat(state.rowModel(0, 0, "kg", Locale.US).workingSetNumber).isNull()
        assertThat(state.rowModel(0, 1, "kg", Locale.US).workingSetNumber).isEqualTo(1)
        assertThat(state.rowModel(0, 3, "kg", Locale.US).workingSetNumber).isEqualTo(3)
        assertThat(state.rowModel(0, 1, "kg", Locale.US).previous).isEqualTo("77.5 × 8")
    }
}
