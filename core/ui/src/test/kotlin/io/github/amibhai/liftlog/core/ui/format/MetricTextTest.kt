package io.github.amibhai.liftlog.core.ui.format

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.progress.ProgressMetric
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.Test
import java.util.Locale

class MetricTextTest {

    private val previousLocale = Locale.getDefault()

    @org.junit.Before
    fun useEnglish() = Locale.setDefault(Locale.US)

    @org.junit.After
    fun restoreLocale() = Locale.setDefault(previousLocale)

    @Test
    fun kilogramsAreShownInTheChosenUnit() {
        val pounds = MetricText(ExerciseType.WEIGHT_REPS, WeightUnit.LB)

        val shown = pounds.displayed(ProgressMetric.TOP_WEIGHT, 100.0)

        assertThat(shown).isWithin(1e-6).of(100 / 0.45359237)
        assertThat(pounds.label(ProgressMetric.TOP_WEIGHT, shown)).isEqualTo("220.5 lb")
        assertThat(pounds.axis(ProgressMetric.VOLUME, 2_500.4)).isEqualTo("2500")
    }

    @Test
    fun timesAndDistancesHaveTheirOwnFormat() {
        val run = MetricText(ExerciseType.DISTANCE_DURATION, WeightUnit.KG)

        assertThat(run.label(ProgressMetric.DURATION, 90.0)).isEqualTo("1:30")
        assertThat(
            run.label(ProgressMetric.DISTANCE, run.displayed(ProgressMetric.DISTANCE, 5_000.0)),
        ).isEqualTo("5 km")
    }

    @Test
    fun repsAreWholeNumbersWithoutAUnit() {
        assertThat(MetricText(ExerciseType.REPS_ONLY, WeightUnit.KG).label(ProgressMetric.REPS, 12.0)).isEqualTo("12")
    }
}
