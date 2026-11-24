package io.github.amibhai.liftlog.feature.history.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.component.RecordRow
import io.github.amibhai.liftlog.core.ui.component.heatmap.BodyHeatmap
import io.github.amibhai.liftlog.core.ui.format.setCountText
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.history.R

/** The records a workout broke. */
@Composable
internal fun RecordsCard(
    state: WorkoutDetailUiState.Ready,
    onOpenExercise: (ExerciseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingXs) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(LiftLogIcons.Trophy),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.history_records),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        for (entry in state.records) {
            RecordRow(entry, state.unit, onClick = { onOpenExercise(entry.record.exerciseId) })
        }
    }
}

/** The body, shaded by what the workout trained. */
@Composable
internal fun MusclesCard(state: WorkoutDetailUiState.Ready, modifier: Modifier = Modifier) {
    val rows = state.muscles.map { stringResource(it.muscle.labelRes()) to setCountText(it.sets) }
    LiftLogCard(modifier = modifier, spacing = LiftLogDimens.spacingS) {
        Text(
            text = stringResource(R.string.history_muscles),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        BodyHeatmap(levels = state.levels, description = rows.joinToString { (muscle, sets) -> "$muscle $sets" })
        for ((muscle, sets) in rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
            ) {
                Text(muscle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(sets, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
