package io.github.amibhai.liftlog.core.ui.format

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.Test
import java.util.Locale

class SetTextTest {

    private val kg = SetText(WeightUnit.KG, Locale.US)
    private val lb = SetText(WeightUnit.LB, Locale.US)
    private val bench = SetValues(Weight(82.5), reps = 8)

    @Test
    fun `a set reads like people write it`() {
        assertThat(kg.summary(ExerciseType.WEIGHT_REPS, bench)).isEqualTo("82.5 kg × 8")
        assertThat(lb.summary(ExerciseType.WEIGHT_REPS, SetValues(Weight.of(45.0, WeightUnit.LB), reps = 5)))
            .isEqualTo("45 lb × 5")
        assertThat(kg.summary(ExerciseType.BODYWEIGHT_ADDED, bench)).isEqualTo("+82.5 kg × 8")
        assertThat(kg.summary(ExerciseType.BODYWEIGHT_ADDED, SetValues(reps = 8))).isEqualTo("× 8")
        assertThat(kg.summary(ExerciseType.BODYWEIGHT_ASSISTED, bench)).isEqualTo("−82.5 kg × 8")
        assertThat(kg.summary(ExerciseType.REPS_ONLY, SetValues(reps = 12))).isEqualTo("× 12")
        assertThat(kg.summary(ExerciseType.DURATION, SetValues(durationSeconds = 90))).isEqualTo("1:30")
    }

    @Test
    fun `distances use the unit that fits the exercise`() {
        val run = SetValues(durationSeconds = 1_710, distanceMeters = 5_250.0)
        assertThat(kg.summary(ExerciseType.DISTANCE_DURATION, run)).isEqualTo("5.25 km · 28:30")
        assertThat(lb.summary(ExerciseType.DISTANCE_DURATION, run)).isEqualTo("3.26 mi · 28:30")
        val carry = SetValues(Weight(40.0), distanceMeters = 20.0)
        assertThat(kg.summary(ExerciseType.WEIGHT_DISTANCE, carry)).isEqualTo("40 kg × 20 m")
        assertThat(kg.summary(ExerciseType.DISTANCE_DURATION, SetValues(durationSeconds = 600))).isEqualTo("10:00")
    }

    @Test
    fun `the narrow column leaves units out`() {
        assertThat(kg.compact(ExerciseType.WEIGHT_REPS, bench)).isEqualTo("82.5 × 8")
        assertThat(kg.compact(ExerciseType.BODYWEIGHT_ADDED, bench)).isEqualTo("+82.5 × 8")
        assertThat(kg.compact(ExerciseType.DISTANCE_DURATION, SetValues(durationSeconds = 60, distanceMeters = 500.0)))
            .isEqualTo("0.5 · 1:00")
    }

    @Test
    fun `fields are formatted one by one`() {
        assertThat(kg.field(ExerciseType.WEIGHT_REPS, bench, SetField.WEIGHT)).isEqualTo("82.5")
        assertThat(kg.field(ExerciseType.BODYWEIGHT_ADDED, bench, SetField.WEIGHT)).isEqualTo("+82.5")
        assertThat(kg.field(ExerciseType.WEIGHT_REPS, bench, SetField.REPS)).isEqualTo("8")
        assertThat(kg.field(ExerciseType.WEIGHT_REPS, SetValues(), SetField.WEIGHT)).isEmpty()
        assertThat(
            kg.field(ExerciseType.DURATION, SetValues(durationSeconds = 45), SetField.DURATION),
        ).isEqualTo("0:45")
    }

    @Test
    fun `every field knows its unit`() {
        assertThat(kg.unitOf(ExerciseType.WEIGHT_REPS, SetField.WEIGHT)).isEqualTo("kg")
        assertThat(lb.unitOf(ExerciseType.WEIGHT_REPS, SetField.WEIGHT)).isEqualTo("lb")
        assertThat(kg.unitOf(ExerciseType.DISTANCE_DURATION, SetField.DISTANCE)).isEqualTo("km")
        assertThat(lb.unitOf(ExerciseType.WEIGHT_DISTANCE, SetField.DISTANCE)).isEqualTo("yd")
        assertThat(kg.unitOf(ExerciseType.WEIGHT_REPS, SetField.REPS)).isNull()
    }

    @Test
    fun `numbers follow the locale`() {
        assertThat(
            SetText(WeightUnit.KG, Locale.GERMANY).summary(ExerciseType.WEIGHT_REPS, bench),
        ).isEqualTo("82,5 kg × 8")
    }
}
