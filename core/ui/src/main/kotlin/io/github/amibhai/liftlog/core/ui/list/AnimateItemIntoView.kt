package io.github.amibhai.liftlog.core.ui.list

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState

/**
 * Scrolls just enough to show the whole item at [index], clear of the content padding (the docked number
 * pad and rest timer). Does nothing when it is already visible.
 */
suspend fun LazyListState.animateItemIntoView(index: Int) {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
    val visibleEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val hiddenBelow = item.offset + item.size - visibleEnd
    when {
        item.offset < 0 -> animateScrollBy(item.offset.toFloat())
        hiddenBelow > 0 -> animateScrollBy(minOf(hiddenBelow, item.offset).toFloat())
    }
}
