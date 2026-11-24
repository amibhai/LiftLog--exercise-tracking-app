package io.github.amibhai.liftlog.core.domain.progress

/** The formulas only hold for up to this many reps (SPEC §8). */
const val MAX_E1RM_REPS = 12

private const val EPLEY_REPS = 30.0
private const val BRZYCKI_FACTOR = 36.0
private const val BRZYCKI_BASE = 37.0

/** The estimated one-rep max of [weightKg] × [reps]; null without weight or outside 1 to 12 reps. */
fun estimateOneRepMax(weightKg: Double, reps: Int, formula: E1rmFormula): Double? = when {
    weightKg <= 0.0 || reps !in 1..MAX_E1RM_REPS -> null
    reps == 1 -> weightKg
    formula == E1rmFormula.EPLEY -> weightKg * (1 + reps / EPLEY_REPS)
    else -> weightKg * BRZYCKI_FACTOR / (BRZYCKI_BASE - reps)
}
