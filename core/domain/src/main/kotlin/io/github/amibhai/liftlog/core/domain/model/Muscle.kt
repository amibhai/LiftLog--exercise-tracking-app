package io.github.amibhai.liftlog.core.domain.model

/** The fixed muscle taxonomy (SPEC §3). Names are stored in the database; never rename one. */
enum class Muscle {
    CHEST,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    LATS,

    /** Upper back and traps. */
    UPPER_BACK,
    LOWER_BACK,
    ABS,
    OBLIQUES,
    GLUTES,
    QUADS,
    HAMSTRINGS,
    ADDUCTORS,
    ABDUCTORS,
    CALVES,
    NECK,
}
