package io.github.amibhai.liftlog.core.domain.model

/** Progression rule of a routine exercise (SPEC §7). Stored by name; more rules arrive in Phase 3. */
enum class ProgressionRuleType {
    DOUBLE_PROGRESSION,
    LINEAR,
    NONE,
}
