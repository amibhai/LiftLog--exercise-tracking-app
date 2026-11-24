package io.github.amibhai.liftlog.core.domain.workout

/** A rep range to progress in (SPEC §7). */
data class RepRange(val min: Int, val max: Int) {
    companion object {
        private val USUAL = listOf(RepRange(1, 2), RepRange(3, 5), RepRange(6, 8), RepRange(8, 12), RepRange(12, 15))

        /** The usual range that [reps] falls in; ranges are read from strength to endurance. */
        fun around(reps: Int): RepRange = when {
            reps <= USUAL.first().max -> RepRange(1, USUAL[1].min)
            else -> USUAL.firstOrNull { reps <= it.max } ?: RepRange(USUAL.last().max, maxOf(reps, ENDURANCE_MAX))
        }

        private const val ENDURANCE_MAX = 20
    }
}
