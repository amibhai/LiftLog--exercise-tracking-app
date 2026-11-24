package io.github.amibhai.liftlog.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import kotlin.math.roundToInt

private val Threshold = 72.dp

/**
 * Swipe towards the end to duplicate (the row springs back), towards the start to delete (it slides out).
 * TalkBack users have the same actions on the set badge.
 */
@Composable
fun SwipeActions(
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Changed right in the drag callback, so that a fast swipe is complete when the finger lifts.
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { Threshold.toPx() }
    val duplicate by rememberUpdatedState(onDuplicate)
    val delete by rememberUpdatedState(onDelete)
    val direction by remember { derivedStateOf { offset.compareTo(0f) } }
    val dragState = rememberDraggableState { delta -> offset += delta }
    Box(modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
        when (direction) {
            1 -> SwipeHint(LiftLogIcons.Copy, MaterialTheme.colorScheme.secondaryContainer, Alignment.CenterStart)
            -1 -> SwipeHint(LiftLogIcons.Delete, MaterialTheme.colorScheme.errorContainer, Alignment.CenterEnd)
        }
        Box(
            Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                    onDragStopped = {
                        val deletes = offset < -threshold
                        if (offset > threshold || deletes) haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        if (offset > threshold) duplicate()
                        // Settles through the drag state, so that touching the row again stops the animation.
                        dragState.drag {
                            val target = if (deletes) -width.toFloat() else 0f
                            animate(offset, target) { value, _ -> dragBy(value - offset) }
                        }
                        if (deletes) delete()
                    },
                ),
        ) { content() }
    }
}

@Composable
private fun BoxScope.SwipeHint(@DrawableRes icon: Int, color: Color, alignment: Alignment) {
    Box(Modifier.matchParentSize().background(color, MaterialTheme.shapes.medium), contentAlignment = alignment) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = LiftLogDimens.spacingXl),
        )
    }
}
