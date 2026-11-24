package io.github.amibhai.liftlog.feature.settings.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.importer.LogSource
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.usecase.ImportChoice
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.ChoiceRow
import io.github.amibhai.liftlog.feature.settings.R
import io.github.amibhai.liftlog.feature.settings.SettingsPage
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun ImportRoute(onBack: () -> Unit, viewModel: ImportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        ImportActions(
            onUnitChange = viewModel::setUnit,
            onSearch = viewModel::search,
            onChoose = viewModel::choose,
            onCreate = viewModel::create,
            onSkip = viewModel::skip,
            onImport = viewModel::import,
        )
    }
    ImportScreen(state = state, actions = actions, onBack = onBack)
}

@Composable
internal fun ImportScreen(
    state: ImportUiState,
    actions: ImportActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(title = stringResource(R.string.import_title), onBack = onBack, modifier = modifier) { padding ->
        val content = Modifier
            .fillMaxSize()
            .padding(padding)
        when (state) {
            ImportUiState.Reading -> LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .padding(padding),
            )

            ImportUiState.Unknown -> EmptyState(
                icon = LiftLogIcons.Info,
                title = stringResource(R.string.import_unknown),
                body = stringResource(R.string.import_unknown_body),
                modifier = content,
            )

            is ImportUiState.Ready -> Names(state, actions, content)

            is ImportUiState.Done -> Done(state, onBack, content)
        }
    }
}

@Composable
private fun Names(state: ImportUiState.Ready, actions: ImportActions, modifier: Modifier = Modifier) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = LiftLogDimens.spacingXl)) {
        item(key = "summary") { Summary(state, actions) }
        items(state.rows, key = { it.name }) { row ->
            NameRow(row, onClick = { if (!state.isImporting) editing = row.name })
        }
        item(key = "import") {
            Button(
                onClick = actions.onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LiftLogDimens.screenGutter)
                    .heightIn(min = LiftLogDimens.primaryAction),
                enabled = !state.isImporting,
            ) {
                Text(pluralStringResource(R.plurals.import_start, state.workouts, state.workouts))
            }
        }
    }
    editing?.let { name ->
        ChooseExerciseDialog(
            name = name,
            found = state.found,
            actions = actions,
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun Summary(state: ImportUiState.Ready, actions: ImportActions, modifier: Modifier = Modifier) {
    val dates = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
        if (state.isImporting) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            text = stringResource(
                R.string.import_summary,
                stringResource(state.source.nameRes()),
                pluralStringResource(R.plurals.data_workouts, state.workouts, state.workouts),
                dates.format(state.first),
                dates.format(state.last),
            ),
            modifier = Modifier.padding(horizontal = LiftLogDimens.screenGutter),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.unreadableRows > 0) {
            Text(
                text = pluralStringResource(R.plurals.import_unreadable, state.unreadableRows, state.unreadableRows),
                modifier = Modifier.padding(horizontal = LiftLogDimens.screenGutter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChoiceRow(
            title = stringResource(if (state.isUnitGuessed) R.string.import_unit_guessed else R.string.import_unit),
            options = WeightUnit.entries,
            selected = state.unit,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onUnitChange,
        )
        Text(
            text = stringResource(R.string.import_names),
            modifier = Modifier.padding(horizontal = LiftLogDimens.screenGutter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NameRow(row: ImportRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        overlineContent = if (row.needsCheck) {
            {
                Text(
                    text = stringResource(R.string.import_check),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        } else {
            null
        },
        supportingContent = {
            Text(
                when (row.choice) {
                    is ImportChoice.Existing -> row.exerciseName.orEmpty()
                    ImportChoice.Create -> stringResource(R.string.import_create_row)
                    ImportChoice.Skip -> stringResource(R.string.import_skip_row)
                },
            )
        },
    ) { Text(row.name) }
}

@Composable
private fun Done(state: ImportUiState.Done, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(LiftLogDimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
    ) {
        LiftLogCard {
            Text(
                text = pluralStringResource(R.plurals.import_done, state.result.added, state.result.added),
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.result.alreadyThere > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.import_already_there,
                        state.result.alreadyThere,
                        state.result.alreadyThere,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LiftLogDimens.primaryAction),
        ) { Text(stringResource(R.string.import_close)) }
    }
}

private fun LogSource.nameRes(): Int = when (this) {
    LogSource.STRONG -> R.string.import_strong
    LogSource.HEVY -> R.string.import_hevy
}

/** What the page can ask for. */
internal class ImportActions(
    val onUnitChange: (WeightUnit) -> Unit,
    val onSearch: (String) -> Unit,
    val onChoose: (String, CatalogExercise) -> Unit,
    val onCreate: (String) -> Unit,
    val onSkip: (String) -> Unit,
    val onImport: () -> Unit,
)
