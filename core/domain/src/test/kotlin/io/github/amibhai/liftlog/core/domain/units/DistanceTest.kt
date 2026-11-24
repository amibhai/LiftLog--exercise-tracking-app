package io.github.amibhai.liftlog.core.domain.units

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DistanceTest {

    @Test
    fun `cardio is measured in kilometres or miles, carries in metres or yards`() {
        assertThat(DistanceUnit.of(ExerciseType.DISTANCE_DURATION, WeightUnit.KG)).isEqualTo(DistanceUnit.KILOMETER)
        assertThat(DistanceUnit.of(ExerciseType.DISTANCE_DURATION, WeightUnit.LB)).isEqualTo(DistanceUnit.MILE)
        assertThat(DistanceUnit.of(ExerciseType.WEIGHT_DISTANCE, WeightUnit.KG)).isEqualTo(DistanceUnit.METER)
        assertThat(DistanceUnit.of(ExerciseType.WEIGHT_DISTANCE, WeightUnit.LB)).isEqualTo(DistanceUnit.YARD)
    }

    @Test
    fun `a typed distance displays back exactly in every unit`() {
        for (unit in DistanceUnit.entries) {
            for (hundredths in 0..5_000) {
                val typed = BigDecimal.valueOf(hundredths.toLong(), 2)
                val meters = unit.toMeters(typed)

                assertThat(unit.display(meters).compareTo(typed)).isEqualTo(0)
            }
        }
    }

    @Test
    fun `conversions use the international definitions`() {
        assertThat(DistanceUnit.MILE.toMeters(BigDecimal.ONE)).isWithin(1e-9).of(1_609.344)
        assertThat(DistanceUnit.YARD.toMeters(BigDecimal.ONE)).isWithin(1e-9).of(0.9144)
        assertThat(DistanceUnit.KILOMETER.display(5_250.0)).isEqualTo(BigDecimal("5.25"))
    }
}
