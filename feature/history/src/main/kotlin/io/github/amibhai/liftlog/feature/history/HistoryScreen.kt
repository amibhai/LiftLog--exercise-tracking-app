package io.github.amibhai.liftlog.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.dateText
import io.github.amibhai.liftlog.core.ui.format.monthText
import io.github.amibhai.liftlog.core.ui.format.workoutTitle
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/** Starts the next page this many items before the end of the list. */
private const val LOAD_AHEAD = 10

@Composable
internal fun HistoryRoute(onOpenWorkout: (WorkoutId) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onOpenWorkout = onOpenWorkout, onLoadMore = viewModel::loadMore)
}

@Composable
internal fun HistoryScreen(
    state: HistoryUiState,
    onOpenWorkout: (WorkoutId) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
    ) { padding ->
        when (state) {
            HistoryUiState.Loading -> Unit

            HistoryUiState.Empty -> EmptyState(
                icon = LiftLogIcons.History,
                title = stringResource(R.string.history_empty_title),
                body = stringResource(R.string.history_empty_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            is HistoryUiState.Ready -> HistoryList(
                state = state,
                onOpenWorkout = onOpenWorkout,
                onLoadMore = onLoadMore,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun HistoryList(
    state: HistoryUiState.Ready,
    onOpenWorkout: (WorkoutId) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val loadMore by rememberUpdatedState(onLoadMore)
    val isNearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_AHEAD
        }
    }
    LaunchedEffect(isNearEnd, state.items.size) { if (isNearEnd && state.canLoadMore) loadMore() }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiftLogDimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
    ) {
        items(state.items, key = { it.key }, contentType = { it::class }) { item ->
            when (item) {
                is HistoryItem.Month -> Text(
                    text = monthText(item.at),
                    modifier = Modifier
                        .padding(top = LiftLogDimens.spacingS)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                is HistoryItem.Entry -> WorkoutCard(item, onClick = { onOpenWorkout(item.id) })
            }
        }
    }
}

@Composable
private fun WorkoutCard(entry: HistoryItem.Entry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(onClick = onClick, modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS, Alignment.End),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = workoutTitle(entry.title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = dateText(entry.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                R.string.history_stats,
                LiftLogFormat.duration(entry.durationSeconds),
                entry.volume,
                pluralStringResource(R.plurals.history_sets, entry.sets, entry.sets),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (line in entry.exercises) ExerciseLineRow(line)
        if (entry.moreExercises > 0) {
            Text(
                text = pluralStringResource(R.plurals.history_more_exercises, entry.moreExercises, entry.moreExercises),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExerciseLineRow(line: ExerciseLine, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
        Text(
            text = stringResource(R.string.history_exercise_line, line.sets, line.name),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        line.best?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
        }
    }
}
