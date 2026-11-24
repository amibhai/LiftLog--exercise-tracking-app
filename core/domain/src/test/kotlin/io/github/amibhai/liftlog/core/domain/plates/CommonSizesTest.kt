package io.github.amibhai.liftlog.core.domain.plates

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.jupiter.api.Test

class CommonSizesTest {

    @Test
    fun `the default bar and plates are among them`() {
        for (unit in WeightUnit.entries) {
            val settings = UserSettings(weightUnit = unit)
            assertThat(unit.commonBars).contains(settings.barWeight)
            assertThat(unit.commonPlates).containsAtLeastElementsIn(settings.plates(unit).map { it.weight })
        }
    }

    @Test
    fun `the bar and the plates belong to the chosen unit`() {
        val changed = UserSettings(weightUnit = WeightUnit.LB).withBarWeight(35.0).withPlates(listOf(Plate(45.0)))

        assertThat(changed.barWeight).isEqualTo(35.0)
        assertThat(changed.plates(WeightUnit.LB)).containsExactly(Plate(45.0))
        assertThat(changed.copy(weightUnit = WeightUnit.KG).barWeight).isEqualTo(20.0)
        assertThat(changed.plates(WeightUnit.KG)).isEqualTo(UserSettings().plates(WeightUnit.KG))
    }
}
