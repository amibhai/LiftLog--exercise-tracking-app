package io.github.amibhai.liftlog.core.ui.preview

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.component.NumberPad
import io.github.amibhai.liftlog.core.ui.component.NumberPadUiState
import io.github.amibhai.liftlog.core.ui.component.RestTimerBar
import io.github.amibhai.liftlog.core.ui.component.RestTimerUiState
import io.github.amibhai.liftlog.core.ui.component.SetFieldUiModel
import io.github.amibhai.liftlog.core.ui.component.SetRow
import io.github.amibhai.liftlog.core.ui.component.SetRowUiModel
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.core.ui.theme.LiftLogTheme

private const val PREVIEW_NOW = 1_700_000_000_000L
private const val PREVIEW_REST_LEFT_MS = 75_000L
private const val PREVIEW_REST_TOTAL_S = 120

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    LiftLogTheme(darkTheme = isSystemInDarkTheme()) {
        Surface { Column(Modifier.padding(LiftLogDimens.spacingS)) { content() } }
    }
}

private fun previewRow(
    id: String,
    type: SetType,
    number: Int?,
    ghost: Boolean,
    completed: Boolean,
    suggestion: String? = null,
) = SetRowUiModel(
    id = id,
    type = type,
    workingSetNumber = number,
    previous = "80 × 8",
    fields = listOf(
        SetFieldUiModel(
            SetField.WEIGHT,
            text = "82.5",
            unit = "kg",
            isGhost = ghost,
            isFocused = suggestion != null,
            note = suggestion,
        ),
        SetFieldUiModel(SetField.REPS, text = "8", unit = null, isGhost = ghost),
    ),
    isCompleted = completed,
)

@PreviewLiftLog
@Composable
private fun SetRowPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXs)) {
            SetRow(previewRow("w", SetType.WARMUP, null, ghost = false, completed = true), onAction = {})
            SetRow(previewRow("1", SetType.WORKING, 1, ghost = false, completed = true), onAction = {})
            SetRow(
                previewRow("2", SetType.WORKING, 2, ghost = true, completed = false, suggestion = "+2.5 kg"),
                onAction = {},
            )
            SetRow(previewRow("d", SetType.DROP, null, ghost = true, completed = false), onAction = {})
        }
    }
}

@PreviewLiftLog
@Composable
private fun NumberPadPreview() {
    PreviewSurface {
        NumberPad(
            state = NumberPadUiState(
                fieldLabel = "Weight (kg)",
                valueText = "82.5",
                stepLabel = "2.5",
                allowsDecimal = true,
                canUsePrevious = true,
                isLastField = false,
            ),
            onKey = {},
        )
    }
}

@PreviewLiftLog
@Composable
private fun RestTimerBarPreview() {
    PreviewSurface {
        RestTimerBar(
            state = RestTimerUiState(
                endsAtMillis = PREVIEW_NOW + PREVIEW_REST_LEFT_MS,
                totalSeconds = PREVIEW_REST_TOTAL_S,
            ),
            onAdjust = {},
            onSkip = {},
            clock = { PREVIEW_NOW },
        )
    }
}

@PreviewLiftLog
@Composable
private fun EmptyStatePreview() {
    PreviewSurface {
        EmptyState(
            icon = LiftLogIcons.History,
            title = "No workouts yet",
            body = "Finished workouts show up here.",
        )
    }
}
