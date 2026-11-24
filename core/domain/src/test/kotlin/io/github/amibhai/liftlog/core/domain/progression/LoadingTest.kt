package io.github.amibhai.liftlog.core.domain.progression

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.info
import org.junit.jupiter.api.Test

class LoadingTest {

    private val kg = Loading(step = 2.5, unit = WeightUnit.KG, lightest = Weight(20.0))
    private val lb = Loading(step = 5.0, unit = WeightUnit.LB)

    @Test
    fun `harder is a step, or the chosen increment`() {
        assertThat(kg.harder(Weight(60.0))).isEqualTo(Weight(62.5))
        assertThat(kg.harder(Weight(60.0), incrementKg = 5.0)).isEqualTo(Weight(65.0))
        assertThat(lb.harder(Weight.of(100.0, WeightUnit.LB)).displayText(WeightUnit.LB)).isEqualTo("105")
    }

    @Test
    fun `an odd weight keeps its offset`() {
        assertThat(kg.harder(Weight(61.0))).isEqualTo(Weight(63.5))
        assertThat(kg.lighter(Weight(61.0), percent = 10)).isEqualTo(Weight(56.0))
    }

    @Test
    fun `lighter takes whole steps off, at least one, and stops at the bar`() {
        assertThat(kg.lighter(Weight(100.0), percent = 10)).isEqualTo(Weight(90.0))
        assertThat(kg.lighter(Weight(22.5), percent = 10)).isEqualTo(Weight(20.0))
        assertThat(kg.lighter(Weight(20.0), percent = 10)).isEqualTo(Weight(20.0))
        assertThat(lb.lighter(Weight.of(225.0, WeightUnit.LB), percent = 10).displayText(WeightUnit.LB))
            .isEqualTo("200")
    }

    @Test
    fun `a part is rounded to whole steps`() {
        assertThat(kg.part(Weight(97.5), percent = 85)).isEqualTo(Weight(82.5))
        assertThat(kg.part(Weight(30.0), percent = 40)).isEqualTo(Weight(20.0))
        assertThat(lb.part(Weight.of(315.0, WeightUnit.LB), percent = 85).displayText(WeightUnit.LB)).isEqualTo("270")
    }

    @Test
    fun `the step and the bar come from the exercise, then from the settings`() {
        val settings = UserSettings()
        val own = info().copy(defaults = ExerciseDefaults(weightStepKg = 1.25, barWeight = Weight(15.0)))

        assertThat(Loading.of(info(), settings)).isEqualTo(kg)
        assertThat(Loading.of(own, settings)).isEqualTo(Loading(1.25, WeightUnit.KG, Weight(15.0)))
        assertThat(Loading.of(info().copy(equipment = Equipment.DUMBBELL), settings))
            .isEqualTo(Loading(2.0, WeightUnit.KG))
        assertThat(Loading.of(info(type = ExerciseType.BODYWEIGHT_ASSISTED), settings).isAssisted).isTrue()
    }
}
