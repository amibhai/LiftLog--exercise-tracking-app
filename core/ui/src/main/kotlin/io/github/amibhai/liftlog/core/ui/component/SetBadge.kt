package io.github.amibhai.liftlog.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/** The set's number or type letter; tapping it opens the set menu. */
@Composable
internal fun SetBadge(
    model: SetRowUiModel,
    onAction: (SetRowAction) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (label, color) = when (model.type) {
        SetType.WARMUP -> stringResource(R.string.ui_set_badge_warmup) to colors.tertiary
        SetType.WORKING -> model.workingSetNumber?.toString().orEmpty() to colors.onSurface
        SetType.DROP -> stringResource(R.string.ui_set_badge_drop) to colors.secondary
        SetType.FAILURE -> stringResource(R.string.ui_set_badge_failure) to colors.error
    }
    val description = if (model.type == SetType.WORKING) {
        stringResource(R.string.ui_set_working, model.workingSetNumber ?: 0)
    } else {
        stringResource(model.type.labelRes())
    }
    val duplicateLabel = stringResource(R.string.ui_set_action_duplicate)
    val deleteLabel = stringResource(R.string.ui_set_action_delete)
    Box(
        modifier = modifier
            .size(LiftLogDimens.minTouchTarget)
            .clip(CircleShape)
            .clickable(onClickLabel = stringResource(R.string.ui_set_action_change_type), onClick = onOpenMenu)
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

/** Set type, duplicate and delete (SPEC §5). */
@Composable
internal fun SetMenu(model: SetRowUiModel, isOpen: Boolean, onAction: (SetRowAction) -> Unit, onDismiss: () -> Unit) {
    val choose: (SetRowAction) -> Unit = {
        onDismiss()
        onAction(it)
    }
    DropdownMenu(expanded = isOpen, onDismissRequest = onDismiss) {
        for (type in listOf(SetType.WORKING, SetType.WARMUP, SetType.DROP, SetType.FAILURE)) {
            DropdownMenuItem(
                text = { Text(stringResource(type.labelRes())) },
                onClick = { choose(SetRowAction.ChangeType(model.id, type)) },
                trailingIcon = {
                    if (type == model.type) Icon(painterResource(LiftLogIcons.Check), contentDescription = null)
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ui_set_action_duplicate)) },
            onClick = { choose(SetRowAction.Duplicate(model.id)) },
            leadingIcon = { Icon(painterResource(LiftLogIcons.Copy), contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ui_set_action_delete)) },
            onClick = { choose(SetRowAction.Delete(model.id)) },
            leadingIcon = { Icon(painterResource(LiftLogIcons.Delete), contentDescription = null) },
        )
    }
}

@StringRes
private fun SetType.labelRes(): Int = when (this) {
    SetType.WARMUP -> R.string.ui_set_warmup
    SetType.WORKING -> R.string.ui_set_type_working
    SetType.DROP -> R.string.ui_set_drop
    SetType.FAILURE -> R.string.ui_set_failure
}
