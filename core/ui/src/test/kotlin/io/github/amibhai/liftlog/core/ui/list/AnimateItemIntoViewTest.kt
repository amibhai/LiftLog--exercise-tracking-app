package io.github.amibhai.liftlog.core.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A 300 dp list of 50 dp items whose last 100 dp are covered by docked controls. */
@RunWith(AndroidJUnit4::class)
class AnimateItemIntoViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val list = LazyListState()

    @Test
    fun revealsAnItemHiddenBehindTheControlsByScrollingJustEnough() {
        showThenReveal(index = 4)

        assertThat(position()).isEqualTo(Position(firstItem = 1, offset = 0))
    }

    @Test
    fun leavesAVisibleItemWhereItIs() {
        showThenReveal(index = 3)

        assertThat(position()).isEqualTo(Position(firstItem = 0, offset = 0))
    }

    @Test
    fun revealsAnItemCutOffAtTheTop() {
        showThenReveal(index = 5, startAt = Position(firstItem = 5, offset = 20))

        assertThat(position()).isEqualTo(Position(firstItem = 5, offset = 0))
    }

    @Test
    fun jumpsToAnItemFarAway() {
        showThenReveal(index = 15)

        assertThat(list.layoutInfo.visibleItemsInfo.map { it.index }).contains(15)
    }

    private fun showThenReveal(index: Int, startAt: Position = Position(0, 0)) {
        lateinit var scope: CoroutineScope
        compose.setContent {
            scope = rememberCoroutineScope()
            LazyColumn(
                modifier = Modifier.height(300.dp),
                state = list,
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                items(20) { Box(Modifier.fillMaxWidth().height(50.dp)) }
            }
        }
        compose.runOnIdle { scope.launch { list.scrollToItem(startAt.firstItem, startAt.offset) } }
        compose.runOnIdle { scope.launch { list.animateItemIntoView(index) } }
        compose.waitForIdle()
    }

    private fun position() = Position(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)

    private data class Position(val firstItem: Int, val offset: Int)
}
