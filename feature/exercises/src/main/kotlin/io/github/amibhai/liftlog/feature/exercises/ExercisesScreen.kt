package io.github.amibhai.liftlog.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ExerciseSummary
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

@Composable
internal fun ExercisesRoute(onOpenExercise: (String) -> Unit, viewModel: ExercisesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The text field owns its state (synchronous typing, survives process death); the ViewModel
    // only sees the query.
    val queryState = rememberTextFieldState()
    LaunchedEffect(queryState) {
        snapshotFlow { queryState.text.toString() }.collect { viewModel.onQueryChange(it) }
    }
    ExercisesScreen(state = state, queryState = queryState, onOpenExercise = onOpenExercise)
}

@Composable
internal fun ExercisesScreen(
    state: ExercisesUiState,
    queryState: TextFieldState,
    onOpenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.exercises_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(queryState = queryState)
            val typed = queryState.text.toString()
            when {
                typed.isBlank() -> SearchPrompt(totalExercises = state.totalExercises)

                state.results.isEmpty() && state.searchedQuery == typed -> EmptyState(
                    icon = LiftLogIcons.Search,
                    title = stringResource(R.string.exercises_no_matches_title, typed.trim()),
                    body = stringResource(R.string.exercises_no_matches_body),
                )

                else -> ResultList(
                    query = state.searchedQuery,
                    results = state.results,
                    onOpenExercise = onOpenExercise,
                )
            }
        }
    }
}

@Composable
private fun SearchField(queryState: TextFieldState, modifier: Modifier = Modifier) {
    OutlinedTextField(
        state = queryState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingS),
        placeholder = { Text(stringResource(R.string.exercises_search_hint)) },
        leadingIcon = { Icon(painterResource(LiftLogIcons.Search), contentDescription = null) },
        trailingIcon = {
            if (queryState.text.isNotEmpty()) {
                IconButton(onClick = { queryState.clearText() }) {
                    Icon(
                        painter = painterResource(LiftLogIcons.Close),
                        contentDescription = stringResource(R.string.exercises_clear_search),
                    )
                }
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun SearchPrompt(totalExercises: Int?, modifier: Modifier = Modifier) {
    EmptyState(
        icon = LiftLogIcons.Exercises,
        title = if (totalExercises != null) {
            pluralStringResource(R.plurals.exercises_prompt_title, totalExercises, totalExercises)
        } else {
            stringResource(R.string.exercises_title)
        },
        body = stringResource(R.string.exercises_prompt_body),
        modifier = modifier,
    )
}

@Composable
private fun ResultList(
    query: String,
    results: List<ExerciseSummary>,
    onOpenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Each query starts at its best match. With one shared state the list would stay anchored to the
    // previous first row and hide better matches that now sort above it.
    val listState = key(query) { rememberLazyListState() }
    LazyColumn(modifier = modifier.fillMaxSize(), state = listState, verticalArrangement = Arrangement.Top) {
        items(results, key = { it.id.value }) { exercise ->
            ExerciseRow(exercise = exercise, onClick = { onOpenExercise(exercise.id.value) })
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val equipment = stringResource(exercise.equipment.labelRes())
    val muscle = exercise.primaryMuscle?.let { stringResource(it.labelRes()) }
    val details = muscle?.let { stringResource(R.string.exercises_row_details, equipment, it) } ?: equipment
    ListItem(
        onClick = onClick,
        modifier = modifier,
        supportingContent = { Text(text = details, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Icon(painterResource(LiftLogIcons.ChevronRight), contentDescription = null) },
    ) {
        Text(exercise.name)
    }
}
