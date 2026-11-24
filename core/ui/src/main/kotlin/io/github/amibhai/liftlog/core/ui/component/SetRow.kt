package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

private const val PREVIOUS_WEIGHT = 1.1f
private const val COMPLETED_TINT_ALPHA = 0.45f
private const val IDLE_CHECK_SCALE = 0.88f
private val FocusBorderWidth = 2.dp

// Coarse steps: rows are measured while scrolling, so the fit search stays short.
private val MinNoteSize = 7.sp
private val NoteSizeStep = 1.sp

/** One set: badge (set type), previous result, value fields and the complete button. */
@Composable
fun SetRow(model: SetRowUiModel, onAction: (SetRowAction) -> Unit, modifier: Modifier = Modifier) {
    var isMenuOpen by remember { mutableStateOf(false) }
    RecordHaptic(model.isRecord)
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
        Box {
            SetBadge(model = model, onAction = onAction, onOpenMenu = { isMenuOpen = true })
            SetMenu(model = model, isOpen = isMenuOpen, onAction = onAction, onDismiss = { isMenuOpen = false })
        }
        PreviousValue(
            previous = model.previous,
            onClick = { onAction(SetRowAction.UsePrevious(model.id)) },
            modifier = Modifier.weight(PREVIOUS_WEIGHT),
        )
        for (field in model.fields) {
            FieldCell(
                field = field,
                onClick = { onAction(SetRowAction.EditField(model.id, field.field)) },
                onLongClick = { isMenuOpen = true },
                modifier = Modifier.weight(1f),
            )
        }
        CompleteButton(
            completed = model.isCompleted,
            onToggle = { onAction(SetRowAction.ToggleComplete(model.id)) },
        )
    }
}

/** Feels the moment a set becomes a record, but not when a record is merely shown again. */
@Composable
private fun RecordHaptic(isRecord: Boolean) {
    val haptics = LocalHapticFeedback.current
    var wasRecord by remember { mutableStateOf(isRecord) }
    LaunchedEffect(isRecord) {
        if (isRecord && !wasRecord) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        wasRecord = isRecord
    }
}

/** Titles over the cells of the set rows below. TalkBack skips it: every row labels its own cells. */
@Composable
fun SetRowHeader(setTitle: String, previousTitle: String, fieldTitles: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LiftLogDimens.spacingS)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        HeaderTitle(setTitle, Modifier.width(LiftLogDimens.minTouchTarget))
        HeaderTitle(previousTitle, Modifier.weight(PREVIOUS_WEIGHT))
        for (title in fieldTitles) HeaderTitle(title, Modifier.weight(1f))
        Spacer(Modifier.width(LiftLogDimens.primaryAction))
    }
}

@Composable
private fun HeaderTitle(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelMedium
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MinNoteSize,
            maxFontSize = style.fontSize,
            stepSize = NoteSizeStep,
        ),
        softWrap = false,
        maxLines = 1,
        style = style,
    )
}

@Composable
private fun PreviousValue(previous: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = if (previous != null) {
        stringResource(R.string.ui_set_previous_description, previous)
    } else {
        stringResource(R.string.ui_set_previous_none_description)
    }
    val useLabel = stringResource(R.string.ui_set_action_use_previous)
    Box(
        modifier = modifier
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = previous != null, onClickLabel = useLabel, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // At large font sizes "77.5 × 8" wraps to "77.5" and "× 8"; the reps never stand alone.
            text = previous?.replace("× ", "×\u00A0") ?: stringResource(R.string.ui_set_previous_none),
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FieldCell(
    field: SetFieldUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val description = field.description()
    val menuLabel = stringResource(R.string.ui_set_action_change_type)
    Surface(
        modifier = modifier
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onLongClickLabel = menuLabel, onLongClick = onLongClick, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
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
            field.note?.let { FieldNote(it) }
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
private fun FieldNote(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelSmall
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MinNoteSize,
            maxFontSize = style.fontSize,
            stepSize = NoteSizeStep,
        ),
        softWrap = false,
        maxLines = 1,
        style = style,
    )
}

/** "Weight 82.5 kg, suggested, +2.5 kg". */
@Composable
private fun SetFieldUiModel.description(): String {
    val label = stringResource(field.labelRes())
    val value = listOfNotNull(text.ifEmpty { null }, unit).joinToString(" ")
    val main = when {
        text.isEmpty() -> label
        isGhost -> stringResource(R.string.ui_set_field_suggested, label, value)
        else -> stringResource(R.string.ui_set_field_value, label, value)
    }
    return listOfNotNull(main, note).joinToString()
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
        shape = CircleShape,
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
