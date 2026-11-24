package io.github.amibhai.liftlog.feature.exercises.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.Attribution
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.R

@Composable
internal fun ExerciseDetailRoute(onBack: () -> Unit, viewModel: ExerciseDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseDetailScreen(state = state, onBack = onBack)
}

@Composable
internal fun ExerciseDetailScreen(state: ExerciseDetailUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text((state as? ExerciseDetailUiState.Loaded)?.exercise?.name.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LiftLogIcons.ArrowBack),
                            contentDescription = stringResource(R.string.exercises_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            ExerciseDetailUiState.Loading -> Unit

            ExerciseDetailUiState.NotFound -> EmptyState(
                icon = LiftLogIcons.Info,
                title = stringResource(R.string.exercises_detail_not_found),
                body = stringResource(R.string.exercises_detail_not_found_body),
                modifier = Modifier.padding(padding),
            )

            is ExerciseDetailUiState.Loaded -> DetailContent(
                exercise = state.exercise,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DetailContent(exercise: ExerciseDetail, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LiftLogDimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        ) {
            Tag(stringResource(exercise.equipment.labelRes()))
            Tag(stringResource(exercise.type.labelRes()))
            Tag(stringResource(exercise.category.labelRes()))
            exercise.mechanic?.let { Tag(stringResource(it.labelRes())) }
            if (exercise.isUnilateral) Tag(stringResource(R.string.exercises_detail_unilateral))
            if (exercise.isCustom) Tag(stringResource(R.string.exercises_detail_custom))
        }
        Section(title = stringResource(R.string.exercises_detail_muscles)) {
            MuscleLine(label = stringResource(R.string.exercises_detail_primary), muscles = exercise.primaryMuscles)
            if (exercise.secondaryMuscles.isNotEmpty()) {
                MuscleLine(
                    label = stringResource(R.string.exercises_detail_secondary),
                    muscles = exercise.secondaryMuscles,
                )
            }
        }
        if (exercise.aliases.isNotEmpty()) {
            Section(title = stringResource(R.string.exercises_detail_aliases)) {
                Text(exercise.aliases.joinToString(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Section(title = stringResource(R.string.exercises_detail_instructions)) {
            if (exercise.instructions.isEmpty()) {
                Text(
                    text = stringResource(R.string.exercises_detail_no_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            exercise.instructions.forEachIndexed { index, step ->
                Text(
                    text = stringResource(R.string.exercises_detail_step, index + 1, step),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        exercise.attribution?.let { AttributionCard(it) }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun MuscleLine(label: String, muscles: List<Muscle>, modifier: Modifier = Modifier) {
    val names = muscles.map { stringResource(it.labelRes()) }.joinToString()
    Text(
        text = stringResource(R.string.exercises_detail_muscle_line, label, names),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

@Composable
private fun Tag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = LiftLogDimens.spacingM, vertical = LiftLogDimens.spacingXs),
        )
    }
}

@Composable
private fun AttributionCard(attribution: Attribution, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LiftLogDimens.spacingL),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXs),
        ) {
            Text(
                text = stringResource(R.string.exercises_detail_source, attribution.sourceName),
                style = MaterialTheme.typography.titleSmall,
            )
            attribution.license?.let {
                Text(stringResource(R.string.exercises_detail_license, it), style = MaterialTheme.typography.bodySmall)
            }
            attribution.author?.let {
                Text(stringResource(R.string.exercises_detail_author, it), style = MaterialTheme.typography.bodySmall)
            }
            attribution.licenseUrl?.let { url ->
                TextButton(onClick = { uriHandler.openUri(url) }) {
                    Text(stringResource(R.string.exercises_detail_view_license))
                }
            }
        }
    }
}
