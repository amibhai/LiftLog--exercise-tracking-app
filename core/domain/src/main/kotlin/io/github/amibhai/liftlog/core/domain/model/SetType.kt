package io.github.amibhai.liftlog.core.domain.model

/** Set types (SPEC §5). Working sets are numbered in the UI; warm-ups are excluded from PRs and volume. */
enum class SetType(val countsTowardRecords: Boolean) {
    WARMUP(countsTowardRecords = false),
    WORKING(countsTowardRecords = true),
    DROP(countsTowardRecords = true),
    FAILURE(countsTowardRecords = true),
    ;

    /** The sets a plan is about: neither warm-ups nor drop sets. */
    val isMain: Boolean get() = this == WORKING || this == FAILURE
}
