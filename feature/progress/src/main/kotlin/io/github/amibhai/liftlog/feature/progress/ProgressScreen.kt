package io.github.amibhai.liftlog.feature.progress

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons

/** Charts, PRs and the muscle heatmap arrive in Phase 2; until then this is the empty state. */
@Composable
internal fun ProgressScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.progress_title)) }) },
    ) { padding ->
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
