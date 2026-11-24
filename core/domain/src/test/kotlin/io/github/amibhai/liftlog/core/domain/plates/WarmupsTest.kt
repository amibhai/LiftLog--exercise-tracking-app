package io.github.amibhai.liftlog.core.domain.plates

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WarmupStep
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.info
import io.github.amibhai.liftlog.core.domain.workout.values
import org.junit.jupiter.api.Test

class WarmupsTest {

    private val settings = UserSettings()

    @Test
    fun `the default is the bar, then 40, 60 and 80 percent`() {
        assertThat(warmupSets(info(), Weight(100.0), settings)).containsExactly(
            values(20.0, 10),
            values(40.0, 5),
            values(60.0, 3),
            values(80.0, 1),
        ).inOrder()
    }

    @Test
    fun `weights are rounded to the plates there are`() {
        assertThat(warmupSets(info(), Weight(72.5), settings).map { it.weight?.kg })
            .containsExactly(20.0, 30.0, 42.5, 57.5).inOrder()
    }

    @Test
    fun `steps that are not heavier than the one before are left out`() {
        assertThat(warmupSets(info(), Weight(30.0), settings)).containsExactly(values(20.0, 10), values(25.0, 1))
        assertThat(warmupSets(info(), Weight(20.0), settings)).isEmpty()
    }

    @Test
    fun `without a bar there is no empty-bar step, and the weight step rounds`() {
        val dumbbell = info().copy(equipment = Equipment.DUMBBELL)

        assertThat(warmupSets(dumbbell, Weight(30.0), settings))
            .containsExactly(values(12.0, 5), values(18.0, 3), values(24.0, 1)).inOrder()
        assertThat(warmupSets(dumbbell, Weight(2.0), settings)).isEmpty()
    }

    @Test
    fun `the steps can be chosen, in any order`() {
        val own = settings.copy(warmupSteps = listOf(WarmupStep(75, 3), WarmupStep(50, 5)))

        assertThat(warmupSets(info(), Weight(100.0), own)).containsExactly(values(50.0, 5), values(75.0, 3)).inOrder()
    }

    @Test
    fun `pounds use the pound plates`() {
        val pounds = settings.copy(weightUnit = WeightUnit.LB)

        assertThat(
            warmupSets(info(), Weight.of(225.0, WeightUnit.LB), pounds).map { it.weight?.displayText(WeightUnit.LB) },
        ).containsExactly("45", "90", "135", "180").inOrder()
    }
}
