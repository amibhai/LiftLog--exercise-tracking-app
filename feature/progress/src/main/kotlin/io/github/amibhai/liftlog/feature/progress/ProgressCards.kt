package io.github.amibhai.liftlog.feature.progress

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.MuscleLoad
import io.github.amibhai.liftlog.core.domain.progress.WeekTotals
import io.github.amibhai.liftlog.core.domain.progress.displayValue
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.StatItem
import io.github.amibhai.liftlog.core.ui.component.chart.ChartPoint
import io.github.amibhai.liftlog.core.ui.component.chart.ColumnChart
import io.github.amibhai.liftlog.core.ui.component.heatmap.BodyHeatmap
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.dateText
import io.github.amibhai.liftlog.core.ui.format.dayMonthText
import io.github.amibhai.liftlog.core.ui.format.labelRes
import io.github.amibhai.liftlog.core.ui.format.recordValueText
import io.github.amibhai.liftlog.core.ui.format.symbol
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.concurrent.TimeUnit

/** How strongly a day of the calendar is coloured, by its sets. */
private const val LIGHT_DAY_SETS = 8
private const val BUSY_DAY_SETS = 16
private const val LIGHT_DAY = 0.4f
private const val BUSY_DAY = 0.7f

/** The gap between cells, as a part of a cell. */
private const val CELL_GAP = 0.18f
private const val CELL_CORNER = 0.25f
private const val DAYS_PER_WEEK = 7

@Composable
internal fun ThisWeekCard(state: ProgressUiState.Ready, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        CardTitle(stringResource(R.string.progress_this_week))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            StatItem(stringResource(R.string.progress_stat_workouts), state.thisWeek.workouts.toString())
            StatItem(stringResource(R.string.progress_stat_sets), state.thisWeek.sets.toString())
            StatItem(
                label = stringResource(R.string.progress_stat_volume),
                value = LiftLogFormat.volume(Weight(state.thisWeek.volumeKg), state.unit),
            )
            StatItem(
                label = stringResource(R.string.progress_stat_time),
                value = LiftLogFormat.duration(state.thisWeek.seconds.toInt()),
            )
        }
        Text(
            text = if (state.streakWeeks > 0) {
                pluralStringResource(R.plurals.progress_streak, state.streakWeeks, state.streakWeeks)
            } else {
                stringResource(R.string.progress_no_streak)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A column per week, a row per day: how much was done. */
@Composable
internal fun CalendarCard(state: ProgressUiState.Ready, modifier: Modifier = Modifier) {
    val workouts = state.calendar.sumOf { week -> week.sets.count { (it ?: 0) > 0 } }
    val description = pluralStringResource(R.plurals.progress_calendar_description, workouts, workouts, CALENDAR_WEEKS)
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        CardTitle(stringResource(R.string.progress_calendar))
        val idle = MaterialTheme.colorScheme.surfaceVariant
        val full = MaterialTheme.colorScheme.primary
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(calendarRatio())
                .clearAndSetSemantics { contentDescription = description },
        ) {
            val cell = size.width / (CALENDAR_WEEKS + (CALENDAR_WEEKS - 1) * CELL_GAP)
            val step = cell * (1 + CELL_GAP)
            state.calendar.forEachIndexed { column, week ->
                week.sets.forEachIndexed { row, sets ->
                    if (sets != null) {
                        drawRoundRect(
                            color = lerp(idle, full, dayLevel(sets)),
                            topLeft = Offset(column * step, row * step),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(cell * CELL_CORNER),
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.progress_calendar_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun calendarRatio(): Float {
    val width = CALENDAR_WEEKS + (CALENDAR_WEEKS - 1) * CELL_GAP
    val height = DAYS_PER_WEEK + (DAYS_PER_WEEK - 1) * CELL_GAP
    return width / height
}

private fun dayLevel(sets: Int): Float = when {
    sets <= 0 -> 0f
    sets <= LIGHT_DAY_SETS -> LIGHT_DAY
    sets <= BUSY_DAY_SETS -> BUSY_DAY
    else -> 1f
}

private enum class WeeklyMetric(@StringRes val label: Int) {
    WORKOUTS(R.string.progress_weekly_workouts),
    VOLUME(R.string.progress_weekly_volume),
    TIME(R.string.progress_weekly_time),
}

@Composable
internal fun WeeklyChartCard(state: ProgressUiState.Ready, modifier: Modifier = Modifier) {
    var metric by rememberSaveable { mutableStateOf(WeeklyMetric.WORKOUTS) }
    val text = weeklyText(metric, state)
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        CardTitle(stringResource(R.string.progress_weekly))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            WeeklyMetric.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == metric,
                    onClick = { metric = option },
                    shape = SegmentedButtonDefaults.itemShape(index, WeeklyMetric.entries.size),
                    label = { Text(stringResource(option.label)) },
                )
            }
        }
        val points = state.weeks.map { week -> ChartPoint(dayMonthText(week.start), weekValue(metric, week, state)) }
        ColumnChart(
            points = points,
            valueText = text.axis,
            description = stringResource(
                R.string.progress_weekly_description,
                stringResource(metric.label),
                text.label(points.last().value),
                pluralStringResource(R.plurals.progress_weeks, CHART_WEEKS, CHART_WEEKS),
            ),
        )
    }
}

private class WeeklyText(val axis: (Double) -> String, val label: (Double) -> String)

@Composable
private fun weeklyText(metric: WeeklyMetric, state: ProgressUiState.Ready): WeeklyText = when (metric) {
    WeeklyMetric.WORKOUTS -> WeeklyText(
        axis = { String.format(Locale.getDefault(), "%.0f", it) },
        label = { String.format(Locale.getDefault(), "%.0f", it) },
    )

    WeeklyMetric.VOLUME -> WeeklyText(
        axis = { LiftLogFormat.volume(Weight(it * state.unit.kilogramsPerUnit), state.unit).substringBefore(' ') },
        label = { LiftLogFormat.volume(Weight(it * state.unit.kilogramsPerUnit), state.unit) },
    )

    WeeklyMetric.TIME -> stringResource(R.string.progress_weekly_hours).let { pattern ->
        WeeklyText(
            axis = { String.format(Locale.getDefault(), "%.1f", it) },
            label = { pattern.format(String.format(Locale.getDefault(), "%.1f", it)) },
        )
    }
}

private fun weekValue(metric: WeeklyMetric, week: WeekTotals, state: ProgressUiState.Ready): Double = when (metric) {
    WeeklyMetric.WORKOUTS -> week.workouts.toDouble()
    WeeklyMetric.VOLUME -> week.volumeKg / state.unit.kilogramsPerUnit
    WeeklyMetric.TIME -> week.seconds.toDouble() / TimeUnit.HOURS.toSeconds(1)
}

@Composable
internal fun MusclesCard(state: ProgressUiState.Ready, modifier: Modifier = Modifier) {
    val trained = state.muscles.filter { it.load != MuscleLoad.NONE }
    val description = trained.map { stringResource(it.muscle.labelRes()) + " " + setsText(it.sets) }
        .ifEmpty { listOf(stringResource(R.string.progress_muscles_none)) }
        .joinToString()
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        CardTitle(stringResource(R.string.progress_muscles))
        BodyHeatmap(levels = state.levels, description = description)
        Text(
            text = stringResource(R.string.progress_muscles_target, "${state.target.first}–${state.target.last}"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (row in trained) MuscleBar(row, state.target)
        val idle = state.muscles.filter { it.load == MuscleLoad.NONE }
        if (idle.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.progress_muscles_not_trained,
                    idle.map { stringResource(it.muscle.labelRes()) }.joinToString(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MuscleBar(row: MuscleRow, target: IntRange, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Row {
            Text(
                text = stringResource(row.muscle.labelRes()),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = setsText(row.sets),
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.load == MuscleLoad.ABOVE) colors.error else colors.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = { (row.sets / target.last).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LiftLogDimens.spacingXs),
            color = when (row.load) {
                MuscleLoad.ABOVE -> colors.error
                MuscleLoad.WITHIN -> colors.primary
                else -> colors.tertiary
            },
        )
    }
}

/** "12 sets", and half sets of secondary muscles count too: "12.5 sets". */
@Composable
private fun setsText(sets: Double): String = stringResource(
    R.string.progress_sets,
    LiftLogFormat.decimal(BigDecimal.valueOf(sets).setScale(1, RoundingMode.HALF_UP)),
)

@Composable
internal fun RecordsCard(
    state: ProgressUiState.Ready,
    onOpenExercise: (ExerciseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingXs) {
        CardTitle(stringResource(R.string.progress_records))
        for (entry in state.records) {
            RecordRow(entry, state, onClick = { onOpenExercise(entry.record.exerciseId) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun RecordRow(
    entry: RecordEntry,
    state: ProgressUiState.Ready,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = LiftLogDimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.exerciseName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    R.string.progress_record_line,
                    stringResource(entry.record.type.labelRes()),
                    dateText(entry.record.achievedAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = recordValueText(
                type = entry.record.type,
                value = entry.record.value,
                weightKg = entry.record.weightKg,
                exerciseType = entry.exerciseType,
                unit = state.unit,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun BodyCard(state: ProgressUiState.Ready, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(onClick = onOpen, modifier = modifier, spacing = LiftLogDimens.spacingS) {
        CardTitle(stringResource(R.string.progress_body))
        val weight = state.bodyWeight
        if (weight == null) {
            Text(stringResource(R.string.progress_body_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            val shown = displayValue(BodyMetricType.BODY_WEIGHT, weight.value, state.unit)
            StatItem(
                label = stringResource(R.string.progress_body_weight, dateText(weight.measuredAt)),
                value = LiftLogFormat.decimal(shown) + " " + state.unit.symbol,
            )
        }
        TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = LiftLogDimens.minTouchTarget)) {
            Text(stringResource(R.string.progress_body_open))
        }
    }
}

@Composable
internal fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}
