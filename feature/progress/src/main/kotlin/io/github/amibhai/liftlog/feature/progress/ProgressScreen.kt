package io.github.amibhai.liftlog.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

@Composable
internal fun ProgressRoute(actions: ProgressActions, viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(state = state, actions = actions)
}

@Composable
internal fun ProgressScreen(state: ProgressUiState, actions: ProgressActions, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.progress_title)) }) },
    ) { padding ->
        when (state) {
            ProgressUiState.Loading -> Unit

            is ProgressUiState.Ready -> if (state.hasWorkouts) {
                Dashboard(state = state, actions = actions, modifier = Modifier.padding(padding))
            } else {
                EmptyState(
                    icon = LiftLogIcons.Progress,
                    title = stringResource(R.string.progress_empty_title),
                    body = stringResource(R.string.progress_empty_body),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
        }
    }
}

@Composable
private fun Dashboard(state: ProgressUiState.Ready, actions: ProgressActions, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(LiftLogDimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
    ) {
        item(key = "week") { ThisWeekCard(state) }
        item(key = "calendar") { CalendarCard(state) }
        item(key = "weeks") { WeeklyChartCard(state) }
        item(key = "muscles") { MusclesCard(state) }
        if (state.records.isNotEmpty()) {
            item(key = "records") { RecordsCard(state, onOpenExercise = actions.onOpenExercise) }
        }
        item(key = "body") { BodyCard(state, onOpen = actions.onOpenBody) }
    }
}
