package io.github.amibhai.liftlog.core.domain.concurrent

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SequentialWorkTest {

    @Test
    fun `work runs in the order it was submitted even when an earlier piece is slower`() = runTest {
        val done = mutableListOf<String>()
        val work = SequentialWork(backgroundScope)

        work.submit {
            delay(100)
            done += "slow"
        }
        work.submit { done += "fast" }
        testScheduler.advanceTimeBy(101)

        assertThat(done).containsExactly("slow", "fast").inOrder()
    }
}
