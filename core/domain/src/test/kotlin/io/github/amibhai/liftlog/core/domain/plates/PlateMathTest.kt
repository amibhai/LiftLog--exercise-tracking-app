package io.github.amibhai.liftlog.core.domain.plates

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.jupiter.api.Test

class PlateMathTest {

    private val kgPlates = UserSettings().platesKg
    private val lbPlates = UserSettings().platesLb
    private val bar = Weight(20.0)

    @Test
    fun `a bar is loaded with the heaviest plates first`() {
        val load = loadPlates(Weight(102.5), bar, kgPlates, WeightUnit.KG)

        assertThat(load.perSide)
            .containsExactly(PlateCount(25.0, 1), PlateCount(15.0, 1), PlateCount(1.25, 1)).inOrder()
        assertThat(load.total).isEqualTo(Weight(102.5))
        assertThat(load.isExact).isTrue()
    }

    @Test
    fun `several plates of one size are counted`() {
        assertThat(loadPlates(Weight(180.0), bar, kgPlates, WeightUnit.KG).perSide)
            .containsExactly(PlateCount(25.0, 3), PlateCount(5.0, 1)).inOrder()
    }

    @Test
    fun `pounds load pounds`() {
        val load = loadPlates(Weight.of(225.0, WeightUnit.LB), Weight.of(45.0, WeightUnit.LB), lbPlates, WeightUnit.LB)

        assertThat(load.perSide).containsExactly(PlateCount(45.0, 2))
        assertThat(load.isExact).isTrue()
    }

    @Test
    fun `an empty bar needs no plates, and nothing is lighter than the bar`() {
        assertThat(loadPlates(Weight(20.0), bar, kgPlates, WeightUnit.KG).perSide).isEmpty()
        val tooLight = loadPlates(Weight(15.0), bar, kgPlates, WeightUnit.KG)
        assertThat(tooLight.total).isEqualTo(bar)
        assertThat(tooLight.isExact).isFalse()
    }

    @Test
    fun `what cannot be loaded exactly is loaded as close below as possible`() {
        val load = loadPlates(Weight(61.0), bar, kgPlates, WeightUnit.KG)

        assertThat(load.total).isEqualTo(Weight(60.0))
        assertThat(load.isExact).isFalse()
    }

    @Test
    fun `pairs run out`() {
        val few = listOf(Plate(20.0, pairs = 1), Plate(10.0, pairs = 2), Plate(5.0, pairs = 1))

        assertThat(loadPlates(Weight(100.0), bar, few, WeightUnit.KG).perSide)
            .containsExactly(PlateCount(20.0, 1), PlateCount(10.0, 2)).inOrder()
        assertThat(loadPlates(Weight(200.0), bar, few, WeightUnit.KG).total).isEqualTo(Weight(110.0))
        assertThat(loadPlates(Weight(60.0), bar, listOf(Plate(20.0, pairs = 0)), WeightUnit.KG).perSide).isEmpty()
    }

    @Test
    fun `the heaviest plate is left out when only others add up`() {
        val odd = listOf(Plate(20.0), Plate(15.0))

        assertThat(loadPlates(Weight(80.0), bar, odd, WeightUnit.KG).perSide).containsExactly(PlateCount(15.0, 2))
    }

    @Test
    fun `every step of the default plates can be loaded`() {
        for (step in 8..120) {
            val weight = Weight(step * 2.5)
            assertThat(loadPlates(weight, bar, kgPlates, WeightUnit.KG).isExact).isTrue()
            val pounds = Weight.of(45.0 + step * 5, WeightUnit.LB)
            assertThat(loadPlates(pounds, Weight.of(45.0, WeightUnit.LB), lbPlates, WeightUnit.LB).isExact).isTrue()
        }
    }

    @Test
    fun `rounding goes to the nearest weight that can be loaded`() {
        assertThat(nearestLoadable(Weight(41.0), bar, kgPlates, WeightUnit.KG)).isEqualTo(Weight(40.0))
        assertThat(nearestLoadable(Weight(42.0), bar, kgPlates, WeightUnit.KG)).isEqualTo(Weight(42.5))
        assertThat(nearestLoadable(Weight(8.0), bar, kgPlates, WeightUnit.KG)).isEqualTo(bar)
        assertThat(nearestLoadable(Weight(55.0), bar, listOf(Plate(20.0)), WeightUnit.KG)).isEqualTo(Weight(60.0))
    }

    @Test
    fun `without plates there is only the bar`() {
        assertThat(nearestLoadable(Weight(50.0), bar, emptyList(), WeightUnit.KG)).isEqualTo(bar)
        assertThat(loadPlates(Weight(50.0), bar, emptyList(), WeightUnit.KG).perSide).isEmpty()
    }
}
