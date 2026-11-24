package io.github.amibhai.liftlog.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/**
 * Everything one set row shows, already formatted. Immutable, so a row only recomposes when its
 * own set changes (SPEC §1: set rows must not recompose unnecessarily).
 */
@Immutable
data class SetRowUiModel(
    val id: String,
    val type: SetType,
    /** 1-based number among working sets; shown in the badge. */
    val workingSetNumber: Int?,
    /** What was done last time for this set, e.g. "80 × 8"; null when there is no history. */
    val previous: String?,
    /** In display order; which fields exist depends on the exercise type. */
    val fields: List<SetFieldUiModel>,
    /** Why the pre-filled weight changed, e.g. "+2.5 kg". */
    val suggestion: String?,
    val isCompleted: Boolean,
)

@Immutable
data class SetFieldUiModel(
    val field: SetField,
    val text: String,
    val unit: String?,
    /** A pre-filled value the user has not touched; completing the set accepts it. */
    val isGhost: Boolean,
    val isFocused: Boolean = false,
)

sealed interface SetRowAction {
    val setId: String

    data class EditField(override val setId: String, val field: SetField) : SetRowAction

    data class ToggleComplete(override val setId: String) : SetRowAction

    data class CycleType(override val setId: String) : SetRowAction

    data class Duplicate(override val setId: String) : SetRowAction

    data class Delete(override val setId: String) : SetRowAction
}

private const val PREVIOUS_WEIGHT = 1.1f
private const val COMPLETED_TINT_ALPHA = 0.45f
private const val IDLE_CHECK_SCALE = 0.88f
private val FocusBorderWidth = 2.dp

// Coarse steps: rows are measured while scrolling, so the fit search stays short.
private val MinSuggestionSize = 7.sp
private val SuggestionSizeStep = 1.sp

/** One logged set: badge (set type), previous result, value fields and the complete button. */
@Composable
fun SetRow(model: SetRowUiModel, onAction: (SetRowAction) -> Unit, modifier: Modifier = Modifier) {
    val completedTint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = COMPLETED_TINT_ALPHA)
    val background by animateColorAsState(
        targetValue = if (model.isCompleted) completedTint else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "setRowBackground",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LiftLogDimens.setRowMinHeight)
            .background(background, MaterialTheme.shapes.medium)
            .padding(horizontal = LiftLogDimens.spacingS, vertical = LiftLogDimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        SetBadge(model = model, onAction = onAction)
        PreviousValue(previous = model.previous, modifier = Modifier.weight(PREVIOUS_WEIGHT))
        for (field in model.fields) {
            FieldCell(
                field = field,
                suggestion = model.suggestion.takeIf { field.field == SetField.WEIGHT },
                onClick = { onAction(SetRowAction.EditField(model.id, field.field)) },
                modifier = Modifier.weight(1f),
            )
        }
        CompleteButton(
            completed = model.isCompleted,
            onToggle = { onAction(SetRowAction.ToggleComplete(model.id)) },
        )
    }
}

@Composable
private fun SetBadge(model: SetRowUiModel, onAction: (SetRowAction) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (label, color) = when (model.type) {
        SetType.WARMUP -> stringResource(R.string.ui_set_badge_warmup) to colors.tertiary
        SetType.WORKING -> (model.workingSetNumber?.toString() ?: "") to colors.onSurface
        SetType.DROP -> stringResource(R.string.ui_set_badge_drop) to colors.secondary
        SetType.FAILURE -> stringResource(R.string.ui_set_badge_failure) to colors.error
    }
    val description = when (model.type) {
        SetType.WARMUP -> stringResource(R.string.ui_set_warmup)
        SetType.WORKING -> stringResource(R.string.ui_set_working, model.workingSetNumber ?: 0)
        SetType.DROP -> stringResource(R.string.ui_set_drop)
        SetType.FAILURE -> stringResource(R.string.ui_set_failure)
    }
    val changeTypeLabel = stringResource(R.string.ui_set_action_change_type)
    val duplicateLabel = stringResource(R.string.ui_set_action_duplicate)
    val deleteLabel = stringResource(R.string.ui_set_action_delete)
    Box(
        modifier = modifier
            .size(LiftLogDimens.minTouchTarget)
            .clip(CircleShape)
            .clickable(onClickLabel = changeTypeLabel) { onAction(SetRowAction.CycleType(model.id)) }
            .semantics {
                contentDescription = description
                customActions = listOf(
                    CustomAccessibilityAction(duplicateLabel) {
                        onAction(SetRowAction.Duplicate(model.id))
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onAction(SetRowAction.Delete(model.id))
                        true
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PreviousValue(previous: String?, modifier: Modifier = Modifier) {
    val description = if (previous != null) {
        stringResource(R.string.ui_set_previous_description, previous)
    } else {
        stringResource(R.string.ui_set_previous_none_description)
    }
    Text(
        // At large font sizes "77.5 × 8" wraps to "77.5" and "× 8"; the reps never stand alone.
        text = previous?.replace("× ", "×\u00A0") ?: stringResource(R.string.ui_set_previous_none),
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FieldCell(field: SetFieldUiModel, suggestion: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(field.field.labelRes())
    val value = listOfNotNull(field.text.ifEmpty { null }, field.unit).joinToString(" ")
    val description = when {
        value.isEmpty() -> label
        field.isGhost -> stringResource(R.string.ui_set_field_suggested, label, value)
        else -> stringResource(R.string.ui_set_field_value, label, value)
    }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = colors.surfaceContainerHigh,
        border = if (field.isFocused) BorderStroke(FocusBorderWidth, colors.primary) else null,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = LiftLogDimens.spacingXs, vertical = 2.dp)
                .clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (suggestion != null) {
                val style = MaterialTheme.typography.labelSmall
                Text(
                    text = suggestion,
                    color = colors.primary,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = MinSuggestionSize,
                        maxFontSize = style.fontSize,
                        stepSize = SuggestionSizeStep,
                    ),
                    softWrap = false,
                    maxLines = 1,
                    style = style,
                )
            }
            Text(
                text = field.text.ifEmpty { stringResource(R.string.ui_set_previous_none) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (field.isGhost) FontWeight.Normal else FontWeight.SemiBold,
                color = if (field.isGhost) colors.onSurfaceVariant else colors.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompleteButton(completed: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (completed) 1f else IDLE_CHECK_SCALE,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "completeScale",
    )
    val colors = MaterialTheme.colorScheme
    FilledIconToggleButton(
        checked = completed,
        onCheckedChange = { checked ->
            if (checked) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onToggle()
        },
        modifier = modifier
            .size(LiftLogDimens.primaryAction)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = colors.surfaceContainerHighest,
            contentColor = colors.onSurfaceVariant,
            checkedContainerColor = colors.primary,
            checkedContentColor = colors.onPrimary,
        ),
    ) {
        Icon(
            painter = painterResource(LiftLogIcons.Check),
            contentDescription = stringResource(R.string.ui_set_complete),
        )
    }
}

@StringRes
private fun SetField.labelRes(): Int = when (this) {
    SetField.WEIGHT -> R.string.ui_field_weight
    SetField.REPS -> R.string.ui_field_reps
    SetField.DURATION -> R.string.ui_field_duration
    SetField.DISTANCE -> R.string.ui_field_distance
}
