package io.github.amibhai.liftlog.feature.progress.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.displayValue
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.component.chart.ChartPoint
import io.github.amibhai.liftlog.core.ui.component.chart.LineChart
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.dateTimeText
import io.github.amibhai.liftlog.core.ui.format.dayMonthText
import io.github.amibhai.liftlog.core.ui.format.metricValueText
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.progress.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun MetricRoute(onBack: () -> Unit, viewModel: MetricViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    MetricScreen(
        state = state,
        snackbars = snackbars,
        onBack = onBack,
        onAdd = viewModel::add,
        onEdit = viewModel::edit,
        onDelete = viewModel::delete,
    )
    UndoSnackbar(state.deleted, snackbars, onUndo = viewModel::undoDelete, onDone = viewModel::onMessageShown)
}

@Composable
internal fun MetricScreen(
    state: MetricUiState,
    snackbars: SnackbarHostState,
    onBack: () -> Unit,
    onAdd: (Double, Long) -> Unit,
    onEdit: (BodyMetric, Double, Long) -> Unit,
    onDelete: (BodyMetricId) -> Unit,
    modifier: Modifier = Modifier,
) {
    // What the dialog is about: a new entry, or one to change.
    var editing by remember { mutableStateOf<Editing?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(state.type.labelRes())) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Editing(null) }) {
                Icon(painterResource(LiftLogIcons.Add), stringResource(R.string.progress_metric_add))
            }
        },
    ) { padding ->
        if (state.entries.isEmpty()) {
            EmptyState(
                icon = LiftLogIcons.Progress,
                title = stringResource(R.string.progress_metric_empty_title),
                body = stringResource(R.string.progress_metric_empty_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(LiftLogDimens.screenGutter),
                verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
            ) {
                item(key = "chart") { MetricChart(state) }
                items(state.entries, key = { it.id.value }) { entry ->
                    EntryRow(
                        entry = entry,
                        state = state,
                        onClick = { editing = Editing(entry) },
                        onDelete = { onDelete(entry.id) },
                    )
                }
            }
        }
    }
    editing?.let { open ->
        MetricEntryDialog(
            type = state.type,
            unit = state.unit,
            entry = open.entry,
            onConfirm = { value, at ->
                editing = null
                open.entry?.let { onEdit(it, value, at) } ?: onAdd(value, at)
            },
            onDismiss = { editing = null },
        )
    }
}

private class Editing(val entry: BodyMetric?)

@Composable
private fun MetricChart(state: MetricUiState, modifier: Modifier = Modifier) {
    val oldestFirst = state.entries.reversed()
    if (oldestFirst.size < 2) return
    val points = oldestFirst.map { metric ->
        val date = Instant.ofEpochMilli(metric.measuredAt).atZone(ZoneId.systemDefault()).toLocalDate()
        ChartPoint(dayMonthText(date), displayValue(state.type, metric.value, state.unit).toDouble())
    }
    val trend = state.trend.map { displayValue(state.type, it.value, state.unit).toDouble() }.ifEmpty { null }
    Column(modifier) {
        if (trend != null) {
            Text(
                text = stringResource(R.string.progress_metric_trend_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LineChart(
            points = points,
            valueText = { LiftLogFormat.decimal(BigDecimal.valueOf(it).setScale(1, RoundingMode.HALF_UP)) },
            description = stringResource(
                R.string.progress_metric_chart_description,
                stringResource(state.type.labelRes()),
                metricValueText(state.type, oldestFirst.last().value, state.unit),
                pluralStringResource(R.plurals.progress_measurements, points.size, points.size),
            ),
            trend = trend,
        )
    }
}

@Composable
private fun EntryRow(
    entry: BodyMetric,
    state: MetricUiState,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        onClick = onClick,
        modifier = modifier.heightIn(min = LiftLogDimens.minTouchTarget),
        supportingContent = { Text(dateTimeText(entry.measuredAt)) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(painterResource(LiftLogIcons.Delete), stringResource(UiR.string.ui_delete))
            }
        },
    ) {
        Text(
            text = metricValueText(state.type, entry.value, state.unit),
            modifier = Modifier.semantics { heading() },
        )
    }
}
