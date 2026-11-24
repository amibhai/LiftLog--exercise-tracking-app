package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.SetType

/**
 * The "Previous" column (SPEC §5): for each current set, the set at the same place last time. Warm-ups
 * only match warm-ups, so an added or skipped warm-up does not shift the working sets.
 */
fun matchPrevious(current: List<SetType>, previous: List<LoggedSet>): List<LoggedSet?> {
    val (warmups, others) = previous.partition { it.type == SetType.WARMUP }
    var nextWarmup = 0
    var nextOther = 0
    return current.map { type ->
        if (type == SetType.WARMUP) warmups.getOrNull(nextWarmup++) else others.getOrNull(nextOther++)
    }
}
