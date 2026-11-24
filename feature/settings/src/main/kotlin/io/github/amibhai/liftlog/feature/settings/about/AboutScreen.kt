package io.github.amibhai.liftlog.feature.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.R
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val MATERIAL_SYMBOLS_URL = "https://fonts.google.com/icons"
private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class AboutViewModel @Inject constructor(repository: ExerciseRepository) : ViewModel() {
    val sources: StateFlow<List<LibrarySource>> = repository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
}

@Composable
internal fun AboutRoute(appVersion: String, onBack: () -> Unit, viewModel: AboutViewModel = hiltViewModel()) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    AboutScreen(sources = sources, appVersion = appVersion, onBack = onBack)
}

/** About & attributions (SPEC §3, §12): data sources with their licenses, icons and open source. */
@Composable
internal fun AboutScreen(
    sources: List<LibrarySource>,
    appVersion: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LiftLogIcons.ArrowBack),
                            contentDescription = stringResource(R.string.settings_back),
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
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        ) {
            Text(stringResource(R.string.about_app_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.about_version, appVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Heading(stringResource(R.string.about_exercise_data))
            sources.forEach { source -> SourceCard(source = source, onOpen = { uriHandler.openUri(source.url) }) }
            Heading(stringResource(R.string.about_icons))
            Text(stringResource(R.string.about_icons_body), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { uriHandler.openUri(MATERIAL_SYMBOLS_URL) }) {
                Text(stringResource(R.string.about_icons_link))
            }
            Heading(stringResource(R.string.about_open_source))
            Text(stringResource(R.string.about_open_source_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(top = LiftLogDimens.spacingM)
            .semantics { heading() },
    )
}

@Composable
private fun SourceCard(source: LibrarySource, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LiftLogDimens.spacingL),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXs),
        ) {
            Text(source.name, style = MaterialTheme.typography.titleMedium)
            source.notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.about_license, source.license), style = MaterialTheme.typography.bodySmall)
            source.revision?.let {
                Text(stringResource(R.string.about_revision, it), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = pluralStringResource(R.plurals.about_exercise_count, source.exerciseCount, source.exerciseCount),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onOpen) { Text(stringResource(R.string.about_visit, source.name)) }
        }
    }
}
