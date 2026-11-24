package io.github.amibhai.liftlog.feature.progress

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.MuscleLoad
import io.github.amibhai.liftlog.core.domain.progress.WeekTotals
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.time.LocalDate

sealed interface ProgressUiState {
    data object Loading : ProgressUiState

    @Immutable
    data class Ready(
        val hasWorkouts: Boolean,
        val unit: WeightUnit,
        val streakWeeks: Int,
        val thisWeek: WeekTotals,
        /** The last weeks, oldest first; weeks without training are zeros. */
        val weeks: List<WeekTotals>,
        /** The calendar heatmap, oldest first. */
        val calendar: List<CalendarWeek>,
        /** This week's sets per muscle, most first; muscles without any are last. */
        val muscles: List<MuscleRow>,
        val target: IntRange,
        /** How far each muscle is worked this week, for the body: 1 is the target's lower end. */
        val levels: Map<Muscle, Float>,
        val records: List<RecordEntry>,
        val bodyWeight: BodyMetric?,
    ) : ProgressUiState
}

/** One column of the calendar: the sets of its days, null for days still to come. */
@Immutable
data class CalendarWeek(val start: LocalDate, val sets: List<Int?>)

@Immutable
data class MuscleRow(val muscle: Muscle, val sets: Double, val load: MuscleLoad)
