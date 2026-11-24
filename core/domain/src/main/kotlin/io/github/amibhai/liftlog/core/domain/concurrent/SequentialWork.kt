package io.github.amibhai.liftlog.core.domain.concurrent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Runs pieces of work one after the other in a scope that outlives the screens submitting them, so that
 * edits keep their order and leaving a screen right after typing never loses one (SPEC §5: crash-proof).
 */
class SequentialWork(scope: CoroutineScope) {
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch { for (work in queue) work() }
    }

    fun submit(work: suspend () -> Unit) {
        queue.trySend(work)
    }
}
