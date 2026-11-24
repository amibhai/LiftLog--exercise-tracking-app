package io.github.amibhai.liftlog.feature.exercises.progress

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.progress.BestMark
import io.github.amibhai.liftlog.core.domain.progress.ProgressMetric
import io.github.amibhai.liftlog.core.domain.progress.ProgressRange
import io.github.amibhai.liftlog.core.domain.progress.RepMax
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.component.chart.ChartPoint
import io.github.amibhai.liftlog.core.ui.component.chart.LineChart
import io.github.amibhai.liftlog.core.ui.format.MetricText
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.format.dateText
import io.github.amibhai.liftlog.core.ui.format.labelRes
import io.github.amibhai.liftlog.core.ui.format.recordValueText
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.R

/** Charts, records and rep maxes of one exercise. */
@Composable
internal fun ProgressTab(
    state: ExerciseProgressUiState?,
    onSelectMetric: (ProgressMetric) -> Unit,
    onSelectRange: (ProgressRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == null || state.sessionCount == 0) {
        EmptyState(
            icon = LiftLogIcons.Progress,
            title = stringResource(R.string.exercises_progress_empty_title),
            body = stringResource(R.string.exercises_progress_empty_body),
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = LiftLogDimens.spacingM),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
    ) {
        item(key = "metrics") { MetricChips(state, onSelectMetric) }
        item(key = "chart") { ProgressChart(state) }
        item(key = "ranges") { RangeChips(state.range, onSelectRange) }
        if (state.marks.isNotEmpty()) {
            item(key = "records") { RecordsSection(state.marks, state.type, state) }
        }
        if (state.repMaxes.isNotEmpty()) {
            item(key = "rep-maxes") { RepMaxSection(state.repMaxes, state) }
        }
    }
}

@Composable
private fun MetricChips(
    state: ExerciseProgressUiState,
    onSelect: (ProgressMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = LiftLogDimens.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        items(state.metrics) { metric ->
            FilterChip(
                selected = metric == state.metric,
                onClick = { onSelect(metric) },
                label = { Text(stringResource(metric.labelRes())) },
            )
        }
    }
}

@Composable
private fun RangeChips(range: ProgressRange, onSelect: (ProgressRange) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = LiftLogDimens.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        items(ProgressRange.entries) { option ->
            FilterChip(
                selected = option == range,
                onClick = { onSelect(option) },
                label = { Text(stringResource(option.labelRes())) },
            )
        }
    }
}

@Composable
private fun ProgressChart(state: ExerciseProgressUiState, modifier: Modifier = Modifier) {
    val metric = state.metric ?: return
    val text = remember(state.type, state.unit) { MetricText(state.type, state.unit) }
    val context = LocalContext.current
    val shown = remember(state.points, text, metric) { state.points.map { it to text.displayed(metric, it.value) } }
    val points = remember(shown, context) {
        shown.map { (point, value) ->
            ChartPoint(
                DateUtils.formatDateTime(
                    context,
                    point.at,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
                ),
                value,
            )
        }
    }
    Column(modifier.padding(horizontal = LiftLogDimens.screenGutter)) {
        if (points.isEmpty()) {
            Text(
                text = stringResource(R.string.exercises_progress_none_in_range),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val latest = shown.last().second
        val first = shown.first().second
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text.label(metric, latest), style = MaterialTheme.typography.headlineMedium)
            if (shown.size > 1) {
                Text(
                    text = changeText(latest - first, metric, text),
                    modifier = Modifier.padding(start = LiftLogDimens.spacingS),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        LineChart(
            points = points,
            valueText = { text.axis(metric, it) },
            description = stringResource(
                R.string.exercises_progress_chart_description,
                stringResource(metric.labelRes()),
                text.label(metric, latest),
                text.label(metric, first),
                pluralStringResource(R.plurals.exercises_progress_workouts, points.size, points.size),
            ),
            wholeNumbers = metric == ProgressMetric.REPS,
        )
    }
}

/** "+5 kg since the first one": signed, so that a drop says so too. */
@Composable
private fun changeText(change: Double, metric: ProgressMetric, text: MetricText): String {
    val amount = text.label(metric, kotlin.math.abs(change))
    return if (change < 0) {
        stringResource(R.string.exercises_progress_down, amount)
    } else {
        stringResource(R.string.exercises_progress_up, amount)
    }
}

@Composable
private fun RecordsSection(
    marks: List<BestMark>,
    type: ExerciseType,
    state: ExerciseProgressUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = LiftLogDimens.screenGutter)) {
        SectionTitle(stringResource(R.string.exercises_progress_records))
        for (mark in marks) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LiftLogDimens.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(mark.type.labelRes()), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = dateText(mark.at),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = recordValueText(mark.type, mark.value, mark.weightKg, type, state.unit),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun RepMaxSection(repMaxes: List<RepMax>, state: ExerciseProgressUiState, modifier: Modifier = Modifier) {
    val text = SetText(state.unit)
    Column(modifier.padding(horizontal = LiftLogDimens.screenGutter)) {
        SectionTitle(stringResource(R.string.exercises_progress_rep_maxes))
        for (row in repMaxes) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LiftLogDimens.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.exercises_progress_rep_max, row.reps),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = text.weight(Weight(row.weightKg)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = repMaxDetails(row),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun repMaxDetails(row: RepMax): String = if (row.doneReps == row.reps) {
    dateText(row.at)
} else {
    stringResource(
        R.string.exercises_progress_done_reps,
        pluralStringResource(R.plurals.exercises_progress_reps, row.doneReps, row.doneReps),
        dateText(row.at),
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .padding(bottom = LiftLogDimens.spacingS)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

private fun ProgressMetric.labelRes(): Int = when (this) {
    ProgressMetric.E1RM -> R.string.exercises_metric_e1rm
    ProgressMetric.TOP_WEIGHT -> R.string.exercises_metric_top_weight
    ProgressMetric.VOLUME -> R.string.exercises_metric_volume
    ProgressMetric.REPS -> R.string.exercises_metric_reps
    ProgressMetric.DURATION -> R.string.exercises_metric_time
    ProgressMetric.DISTANCE -> R.string.exercises_metric_distance
}

private fun ProgressRange.labelRes(): Int = when (this) {
    ProgressRange.MONTH -> R.string.exercises_range_month
    ProgressRange.QUARTER -> R.string.exercises_range_quarter
    ProgressRange.HALF_YEAR -> R.string.exercises_range_half_year
    ProgressRange.YEAR -> R.string.exercises_range_year
    ProgressRange.ALL -> R.string.exercises_range_all
}
