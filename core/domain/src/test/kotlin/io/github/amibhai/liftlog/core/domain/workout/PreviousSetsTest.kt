package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.SetType
import org.junit.jupiter.api.Test

class PreviousSetsTest {

    private val warmup = logged(40.0, 10, SetType.WARMUP)
    private val first = logged(80.0, 8)
    private val second = logged(80.0, 7)

    @Test
    fun `each set shows the same set of last time`() {
        assertThat(matchPrevious(listOf(SetType.WORKING, SetType.WORKING), listOf(first, second)))
            .containsExactly(first, second).inOrder()
    }

    @Test
    fun `warm-ups are matched with warm-ups`() {
        assertThat(matchPrevious(listOf(SetType.WORKING, SetType.WORKING), listOf(warmup, first, second)))
            .containsExactly(first, second).inOrder()
        assertThat(matchPrevious(listOf(SetType.WARMUP, SetType.WARMUP, SetType.WORKING), listOf(warmup, first)))
            .containsExactly(warmup, null, first).inOrder()
    }

    @Test
    fun `extra sets have no previous value`() {
        assertThat(matchPrevious(listOf(SetType.WORKING, SetType.DROP, SetType.WORKING), listOf(first, second)))
            .containsExactly(first, second, null).inOrder()
    }
}
