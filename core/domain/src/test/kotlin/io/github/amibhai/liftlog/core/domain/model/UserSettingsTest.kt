package io.github.amibhai.liftlog.core.domain.model

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.jupiter.api.Test

class UserSettingsTest {

    private val defaults = UserSettings()

    @Test
    fun `defaults follow SPEC units section`() {
        assertThat(defaults.weightUnit).isEqualTo(WeightUnit.KG)
        assertThat(
            defaults.platesKg.map { it.weight },
        ).containsExactly(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25).inOrder()
        assertThat(defaults.platesLb.map { it.weight }).containsExactly(45.0, 35.0, 25.0, 10.0, 5.0, 2.5).inOrder()
    }

    @Test
    fun `dark theme is the default`() {
        assertThat(defaults.themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `default weight step is 2_5 kg or 5 lb and 2 kg for dumbbells`() {
        assertThat(defaults.weightStep(Equipment.BARBELL, WeightUnit.KG)).isEqualTo(2.5)
        assertThat(defaults.weightStep(Equipment.DUMBBELL, WeightUnit.KG)).isEqualTo(2.0)
        assertThat(defaults.weightStep(Equipment.BARBELL, WeightUnit.LB)).isEqualTo(5.0)
        assertThat(defaults.weightStep(Equipment.DUMBBELL, WeightUnit.LB)).isEqualTo(5.0)
    }

    @Test
    fun `default bar weight is 20 kg or 45 lb for barbells only`() {
        assertThat(defaults.barWeight(Equipment.BARBELL, WeightUnit.KG)).isEqualTo(Weight.of(20.0, WeightUnit.KG))
        assertThat(defaults.barWeight(Equipment.BARBELL, WeightUnit.LB)).isEqualTo(Weight.of(45.0, WeightUnit.LB))
        assertThat(defaults.barWeight(Equipment.DUMBBELL, WeightUnit.KG)).isNull()
    }

    @Test
    fun `plate inventory follows the active unit`() {
        assertThat(defaults.plates(WeightUnit.KG)).isEqualTo(defaults.platesKg)
        assertThat(defaults.plates(WeightUnit.LB)).isEqualTo(defaults.platesLb)
    }
}
