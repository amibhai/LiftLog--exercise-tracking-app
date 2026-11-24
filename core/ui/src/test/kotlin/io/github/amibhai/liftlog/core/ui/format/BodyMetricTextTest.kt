package io.github.amibhai.liftlog.core.ui.format

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.Test

class BodyMetricTextTest {

    @Test
    fun everyKindIsShownInItsUnit() {
        assertThat(metricValueText(BodyMetricType.BODY_WEIGHT, 81.5, WeightUnit.KG)).isEqualTo("81.5 kg")
        assertThat(metricValueText(BodyMetricType.WAIST, 84.0, WeightUnit.KG)).isEqualTo("84 cm")
        assertThat(metricValueText(BodyMetricType.WAIST, 25.4, WeightUnit.LB)).isEqualTo("10 in")
        assertThat(metricValueText(BodyMetricType.BODY_FAT, 15.0, WeightUnit.LB)).isEqualTo("15 %")
    }
}
