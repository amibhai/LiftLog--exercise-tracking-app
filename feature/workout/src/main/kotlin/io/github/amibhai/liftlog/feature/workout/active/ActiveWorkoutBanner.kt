package io.github.amibhai.liftlog.feature.workout.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.workoutTitle
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.workout.R
import kotlinx.coroutines.delay

private const val MILLIS_PER_SECOND = 1_000L

/** "Resume workout", shown above the tabs while a workout is in progress (SPEC §5). */
@Composable
fun ActiveWorkoutBanner(
    onOpen: (WorkoutId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ActiveWorkoutBanner(state = state, onOpen = onOpen, modifier = modifier)
}

@Composable
internal fun ActiveWorkoutBanner(
    state: ActiveWorkoutUiState?,
    onOpen: (WorkoutId) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state != null,
        modifier = modifier,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        // Keeps showing the last workout while the banner slides away.
        state?.let { BannerContent(it, onOpen) }
    }
}

@Composable
private fun BannerContent(state: ActiveWorkoutUiState, onOpen: (WorkoutId) -> Unit, modifier: Modifier = Modifier) {
    val now by produceState(System.currentTimeMillis(), state.startedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(MILLIS_PER_SECOND - (value - state.startedAt).mod(MILLIS_PER_SECOND))
        }
    }
    val elapsed = LiftLogFormat.duration(((now - state.startedAt) / MILLIS_PER_SECOND).toInt())
    val rest = state.restEndsAt?.let { ((it - now + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt() }
    val sets = pluralStringResource(R.plurals.workout_banner_sets, state.completedSets, state.completedSets)
    val details = if (rest != null && rest > 0) {
        stringResource(R.string.workout_banner_resting, elapsed, sets, LiftLogFormat.duration(rest))
    } else {
        stringResource(R.string.workout_banner_details, elapsed, sets)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.workout_banner_resume),
                role = Role.Button,
                onClick = { onOpen(state.id) },
            ),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = LiftLogDimens.primaryAction)
                .padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        ) {
            Icon(painterResource(LiftLogIcons.Exercises), contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(workoutTitle(state.title), style = MaterialTheme.typography.titleSmall)
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.workout_banner_resume), style = MaterialTheme.typography.labelLarge)
            Icon(painterResource(LiftLogIcons.ChevronRight), contentDescription = null)
        }
    }
}
