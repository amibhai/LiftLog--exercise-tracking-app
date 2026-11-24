package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

private val DragElevation = 6.dp

/** [tag] is a short mark before the title, such as a superset letter. */
@Immutable
data class ReorderItem(val id: String, val title: String, val tag: String? = null)

/** Puts items in a new order with drag handles (SPEC §5); TalkBack moves them with actions. */
@Composable
fun ReorderDialog(
    title: String,
    items: List<ReorderItem>,
    onConfirm: (ids: List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reorder = remember { ReorderState(listState, items) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            LazyColumn(state = listState) {
                items(reorder.items, key = { it.id }) { item -> ReorderRow(item, reorder) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reorder.items.map { it.id }) }) { Text(stringResource(R.string.ui_done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    )
}

@Composable
private fun LazyItemScope.ReorderRow(item: ReorderItem, reorder: ReorderState, modifier: Modifier = Modifier) {
    val isDragged = reorder.draggedId == item.id
    val handle = stringResource(R.string.ui_reorder_handle, item.title)
    val up = stringResource(R.string.ui_move_up)
    val down = stringResource(R.string.ui_move_down)
    Surface(
        modifier = modifier
            .then(if (isDragged) Modifier.zIndex(1f) else Modifier.animateItem())
            .graphicsLayer { translationY = if (isDragged) reorder.offset else 0f },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = if (isDragged) DragElevation else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = LiftLogDimens.primaryAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item.tag?.let {
                Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(LiftLogDimens.spacingS))
            }
            Text(text = item.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(
                painter = painterResource(LiftLogIcons.DragHandle),
                contentDescription = null,
                modifier = Modifier
                    .semantics {
                        contentDescription = handle
                        customActions = listOf(
                            CustomAccessibilityAction(up) { reorder.moveBy(item.id, -1) },
                            CustomAccessibilityAction(down) { reorder.moveBy(item.id, 1) },
                        )
                    }
                    .pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { reorder.start(item.id) },
                            onDragEnd = reorder::stop,
                            onDragCancel = reorder::stop,
                        ) { change, amount ->
                            change.consume()
                            reorder.drag(amount.y)
                        }
                    }
                    .padding(LiftLogDimens.spacingM),
            )
        }
    }
}

@Stable
private class ReorderState(private val list: LazyListState, initial: List<ReorderItem>) {
    var items by mutableStateOf(initial)
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set

    /** How far the dragged row is from where the list puts it. */
    var offset by mutableFloatStateOf(0f)
        private set

    fun start(id: String) {
        draggedId = id
        offset = 0f
    }

    fun stop() {
        draggedId = null
        offset = 0f
    }

    fun drag(amount: Float) {
        offset += amount
        val (dragged, target) = rowUnderDragged() ?: return
        if (moveTo(dragged.index, target.index)) {
            // Keeps the row under the finger, also when the rows it passes are taller or shorter than it.
            val newOffset = if (target.index > dragged.index) {
                target.offset + target.size - dragged.size
            } else {
                target.offset
            }
            offset += dragged.offset - newOffset
        }
    }

    /** The dragged row and the one its center is over. */
    private fun rowUnderDragged(): Pair<LazyListItemInfo, LazyListItemInfo>? {
        val visible = list.layoutInfo.visibleItemsInfo
        // The layout shows a move one frame later; until then it still has the old order.
        val dragged = visible.find { it.key == draggedId }?.takeIf { items.getOrNull(it.index)?.id == draggedId }
            ?: return null
        val center = dragged.offset + offset + dragged.size / 2f
        val target = visible.find { it.key != draggedId && center >= it.offset && center < it.offset + it.size }
        return target?.let { dragged to it }
    }

    fun moveBy(id: String, delta: Int): Boolean {
        val from = items.indexOfFirst { it.id == id }
        return moveTo(from, from + delta)
    }

    private fun moveTo(from: Int, to: Int): Boolean {
        if (from !in items.indices || to !in items.indices) return false
        items = items.toMutableList().apply { add(to, removeAt(from)) }
        return true
    }
}
