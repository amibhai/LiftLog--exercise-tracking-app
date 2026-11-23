package io.github.amibhai.liftlog.core.domain.model

/** The fixed equipment taxonomy (SPEC §3). Names are stored in the database; never rename one. */
enum class Equipment {
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    MACHINE,
    CABLE,
    SMITH,
    BODYWEIGHT,
    BAND,
    EZ_BAR,
    TRAP_BAR,
    PLATE,
    OTHER,
}
