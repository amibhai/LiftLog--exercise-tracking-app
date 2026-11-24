package io.github.amibhai.liftlog.feature.exercises

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.ui.component.SearchField
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.list.BrowseRow
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseList
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseListUiState
import io.github.amibhai.liftlog.feature.exercises.list.FilterRow

@Composable
internal fun ExercisesRoute(
    onOpenExercise: (ExerciseId) -> Unit,
    onCreateExercise: (name: String) -> Unit,
    viewModel: ExercisesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The text field owns its state (synchronous typing, survives process death); the ViewModel
    // only sees the query.
    val query = rememberTextFieldState()
    LaunchedEffect(query) { snapshotFlow { query.text.toString() }.collect(viewModel::onQueryChange) }
    val actions = remember(viewModel) {
        ExerciseListActions(
            onMuscleChange = viewModel::onMuscleChange,
            onEquipmentChange = viewModel::onEquipmentChange,
            onOpenExercise = onOpenExercise,
            onCreateExercise = onCreateExercise,
        )
    }
    ExercisesScreen(state = state, query = query, actions = actions)
}

@Composable
internal fun ExercisesScreen(
    state: ExerciseListUiState,
    query: TextFieldState,
    actions: ExerciseListActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.exercises_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { actions.onCreateExercise("") },
                icon = { Icon(painterResource(LiftLogIcons.Add), contentDescription = null) },
                text = { Text(stringResource(R.string.exercises_new)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                state = query,
                hint = if (state.totalExercises > 0) {
                    pluralStringResource(
                        R.plurals.exercises_search_count,
                        state.totalExercises,
                        state.totalExercises,
                    )
                } else {
                    stringResource(R.string.exercises_search_hint)
                },
                modifier = Modifier.padding(
                    horizontal = LiftLogDimens.screenGutter,
                    vertical = LiftLogDimens.spacingXs,
                ),
            )
            FilterRow(state.filter, actions.onMuscleChange, actions.onEquipmentChange)
            ExerciseList(
                state = state,
                typed = query.text.toString(),
                onCreate = actions.onCreateExercise,
            ) { row -> BrowseRow(row, onClick = { actions.onOpenExercise(row.id) }) }
        }
    }
}
