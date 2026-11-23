package io.github.amibhai.liftlog.core.domain.model

/** Broad kind of exercise. Powerlifting variants count as [STRENGTH]; stretches as [MOBILITY]. */
enum class ExerciseCategory {
    STRENGTH,
    OLYMPIC,
    PLYOMETRIC,
    STRONGMAN,
    CARDIO,
    MOBILITY,
}
