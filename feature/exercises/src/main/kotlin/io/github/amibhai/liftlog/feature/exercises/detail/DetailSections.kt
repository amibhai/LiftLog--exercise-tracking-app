package io.github.amibhai.liftlog.feature.exercises.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.amibhai.liftlog.core.domain.model.Attribution
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseImage
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.TextLink
import io.github.amibhai.liftlog.core.ui.format.relativeDays
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.R

private val ImageHeight = 200.dp

/** The pictures side by side; they are decorative, the steps are in the text. */
@Composable
internal fun ExerciseImages(images: List<ExerciseImage>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = LiftLogDimens.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        items(images, key = { it.url }) { image ->
            AsyncImage(
                model = image.url,
                contentDescription = null,
                modifier = Modifier.height(ImageHeight).clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.FillHeight,
            )
        }
    }
}

@Composable
internal fun Tags(exercise: ExerciseDetail, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        Tag(stringResource(exercise.equipment.labelRes()))
        Tag(stringResource(exercise.type.labelRes()))
        Tag(stringResource(exercise.category.labelRes()))
        exercise.mechanic?.let { Tag(stringResource(it.labelRes())) }
        if (exercise.isUnilateral) Tag(stringResource(R.string.exercises_detail_unilateral))
        if (exercise.isCustom) Tag(stringResource(R.string.exercises_detail_custom))
        if (exercise.isArchived) Tag(stringResource(R.string.exercises_detail_hidden))
    }
}

@Composable
internal fun Muscles(exercise: ExerciseDetail, modifier: Modifier = Modifier) {
    Section(stringResource(R.string.exercises_detail_muscles), modifier) {
        MuscleLine(stringResource(R.string.exercises_detail_primary), exercise.primaryMuscles)
        if (exercise.secondaryMuscles.isNotEmpty()) {
            MuscleLine(stringResource(R.string.exercises_detail_secondary), exercise.secondaryMuscles)
        }
        if (exercise.aliases.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.exercises_detail_muscle_line,
                    stringResource(R.string.exercises_detail_aliases),
                    exercise.aliases.joinToString(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun History(sessions: List<SessionUiModel>, modifier: Modifier = Modifier) {
    Section(stringResource(R.string.exercises_detail_history), modifier) {
        if (sessions.isEmpty()) MutedText(stringResource(R.string.exercises_detail_no_history))
        for (session in sessions) {
            Column(Modifier.padding(vertical = LiftLogDimens.spacingXs)) {
                Text(relativeDays(session.daysAgo), style = MaterialTheme.typography.labelLarge)
                MutedText(session.sets.joinToString())
            }
        }
    }
}

@Composable
internal fun Instructions(exercise: ExerciseDetail, modifier: Modifier = Modifier) {
    Section(stringResource(R.string.exercises_detail_instructions), modifier) {
        if (exercise.instructions.isEmpty()) MutedText(stringResource(R.string.exercises_detail_no_instructions))
        exercise.instructions.forEachIndexed { index, step ->
            Text(
                text = stringResource(R.string.exercises_detail_step, index + 1, step),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun AttributionCard(attribution: Attribution, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingXs) {
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
            TextLink(
                text = stringResource(R.string.exercises_detail_view_license),
                onClick = { uriHandler.openUri(url) },
            )
        }
    }
}

@Composable
internal fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
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
    val names = muscles.map { stringResource(it.labelRes()) }
    Text(
        text = stringResource(R.string.exercises_detail_muscle_line, label, names.joinToString()),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

@Composable
private fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
