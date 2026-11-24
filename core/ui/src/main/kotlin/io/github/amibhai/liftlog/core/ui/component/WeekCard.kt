package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.amibhai.liftlog.core.domain.progress.WeekTotals
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/** This week's training so far and the streak (SPEC §8, §11). With [onClick] the card opens more. */
@Composable
fun WeekCard(
    week: WeekTotals,
    streakWeeks: Int,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable ColumnScope.() -> Unit = { WeekContent(week, streakWeeks, unit) }
    if (onClick == null) {
        LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM, content = content)
    } else {
        LiftLogCard(onClick = onClick, modifier = modifier, spacing = LiftLogDimens.spacingM, content = content)
    }
}

@Composable
private fun ColumnScope.WeekContent(week: WeekTotals, streakWeeks: Int, unit: WeightUnit) {
    Text(
        text = stringResource(R.string.ui_week_title),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXl),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        StatItem(stringResource(R.string.ui_week_workouts), week.workouts.toString())
        StatItem(stringResource(R.string.ui_week_sets), week.sets.toString())
        StatItem(stringResource(R.string.ui_week_volume), LiftLogFormat.volume(Weight(week.volumeKg), unit))
        StatItem(stringResource(R.string.ui_week_time), LiftLogFormat.duration(week.seconds.toInt()))
    }
    Text(
        text = if (streakWeeks > 0) {
            pluralStringResource(R.plurals.ui_week_streak, streakWeeks, streakWeeks)
        } else {
            stringResource(R.string.ui_week_no_streak)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
