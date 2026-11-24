package io.github.amibhai.liftlog.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

@Composable
internal fun HomeRoute(
    onOpenSettings: () -> Unit,
    onBrowseExercises: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onOpenSettings = onOpenSettings, onBrowseExercises = onBrowseExercises)
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onBrowseExercises: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(LiftLogIcons.Settings),
                            contentDescription = stringResource(R.string.home_open_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(LiftLogDimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
        ) {
            StartWorkoutCard()
            LibraryCard(exerciseCount = state.exerciseCount, onBrowseExercises = onBrowseExercises)
        }
    }
}

@Composable
private fun StartWorkoutCard(modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        Text(stringResource(R.string.home_start_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.home_start_coming),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LiftLogDimens.primaryAction),
        ) {
            Text(stringResource(R.string.home_start_empty))
        }
    }
}

@Composable
private fun LibraryCard(exerciseCount: Int?, onBrowseExercises: () -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingM) {
        Text(stringResource(R.string.home_library_title), style = MaterialTheme.typography.titleLarge)
        if (exerciseCount != null) {
            Text(
                text = pluralStringResource(R.plurals.home_library_count, exerciseCount, exerciseCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = onBrowseExercises,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LiftLogDimens.minTouchTarget),
        ) {
            Text(stringResource(R.string.home_library_browse))
        }
    }
}
