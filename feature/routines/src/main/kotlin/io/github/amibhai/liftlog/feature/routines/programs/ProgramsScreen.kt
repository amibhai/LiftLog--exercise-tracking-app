package io.github.amibhai.liftlog.feature.routines.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.program.ProgramTemplate
import io.github.amibhai.liftlog.core.domain.program.TemplateKey
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.routines.R
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun ProgramsRoute(navigation: ProgramsNavigation, viewModel: ProgramsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.openProgram) {
        state.openProgram?.let {
            navigation.onOpenProgram(it)
            viewModel.onProgramOpened()
        }
    }
    ProgramsScreen(state = state, navigation = navigation, onNewProgram = viewModel::newProgram)
}

@Composable
internal fun ProgramsScreen(
    state: ProgramsUiState,
    navigation: ProgramsNavigation,
    onNewProgram: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.programs_title)) },
                navigationIcon = {
                    IconButton(onClick = navigation.onBack) {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(LiftLogDimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        ) {
            val programs = state.programs.orEmpty()
            if (programs.isNotEmpty()) {
                item(key = "yours") { SectionTitle(stringResource(R.string.programs_yours)) }
                items(programs, key = { it.id.value }) { program ->
                    ProgramCard(program, onOpen = { navigation.onOpenProgram(program.id) })
                }
            }
            item(key = "templates") { SectionTitle(stringResource(R.string.programs_templates)) }
            items(state.templates, key = { it.key.name }) { template ->
                TemplateCard(template, onOpen = { navigation.onOpenTemplate(template.key) })
            }
            item(key = "new") {
                FilledTonalButton(
                    onClick = onNewProgram,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LiftLogDimens.primaryAction),
                ) { Text(stringResource(R.string.programs_new)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun ProgramCard(program: Program, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(onClick = onOpen, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = program.name.ifBlank { stringResource(R.string.programs_untitled) },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            if (program.isActive) {
                Text(
                    text = stringResource(R.string.programs_active),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = program.days.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TemplateCard(template: ProgramTemplate, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    LiftLogCard(onClick = onOpen, modifier = modifier) {
        Text(stringResource(template.key.nameRes()), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(template.key.descriptionRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = pluralStringResource(R.plurals.programs_days, template.days.size, template.days.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Where the programs lead. */
internal class ProgramsNavigation(
    val onBack: () -> Unit,
    val onOpenProgram: (ProgramId) -> Unit,
    val onOpenTemplate: (TemplateKey) -> Unit,
)
