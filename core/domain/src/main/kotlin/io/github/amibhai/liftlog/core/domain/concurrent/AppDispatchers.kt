package io.github.amibhai.liftlog.core.domain.concurrent

import kotlinx.coroutines.CoroutineDispatcher

/** Where work runs; tests pass a test dispatcher. */
data class AppDispatchers(val default: CoroutineDispatcher)
