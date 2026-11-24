package io.github.amibhai.liftlog.core.domain.units

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetValues
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FieldValuesTest {

    @Test
    fun `a typed pound weight is stored in kilograms and reads back as typed`() {
        val entered = SetValues.EMPTY.enter(SetField.WEIGHT, BigDecimal("45"), ExerciseType.WEIGHT_REPS, WeightUnit.LB)

        assertThat(entered.weight!!.kg).isWithin(1e-9).of(20.41165665)
        assertThat(entered.shown(SetField.WEIGHT, ExerciseType.WEIGHT_REPS, WeightUnit.LB)).isEqualTo(BigDecimal("45"))
    }

    @Test
    fun `distances use the unit of the exercise`() {
        val run = SetValues.EMPTY.enter(
            SetField.DISTANCE,
            BigDecimal("5.25"),
            ExerciseType.DISTANCE_DURATION,
            WeightUnit.KG,
        )

        assertThat(run.distanceMeters).isWithin(1e-9).of(5_250.0)
        assertThat(run.shown(SetField.DISTANCE, ExerciseType.DISTANCE_DURATION, WeightUnit.KG))
            .isEqualTo(BigDecimal("5.25"))
    }

    @Test
    fun `reps and seconds are whole numbers`() {
        val reps = SetValues.EMPTY.enter(SetField.REPS, BigDecimal("8"), ExerciseType.WEIGHT_REPS, WeightUnit.KG)
        val time = reps.enter(SetField.DURATION, BigDecimal("90"), ExerciseType.DURATION, WeightUnit.KG)

        assertThat(time.reps).isEqualTo(8)
        assertThat(time.durationSeconds).isEqualTo(90)
        assertThat(time.shown(SetField.DURATION, ExerciseType.DURATION, WeightUnit.KG)).isEqualTo(BigDecimal(90))
    }

    @Test
    fun `clearing a field leaves the others`() {
        val both = SetValues(Weight(80.0), reps = 8)

        assertThat(both.enter(SetField.WEIGHT, null, ExerciseType.WEIGHT_REPS, WeightUnit.KG))
            .isEqualTo(SetValues(reps = 8))
        assertThat(SetValues.EMPTY.shown(SetField.WEIGHT, ExerciseType.WEIGHT_REPS, WeightUnit.KG)).isNull()
    }
}
