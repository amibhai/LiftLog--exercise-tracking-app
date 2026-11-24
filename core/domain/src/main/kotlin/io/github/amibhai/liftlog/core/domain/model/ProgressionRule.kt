package io.github.amibhai.liftlog.core.domain.model

/** Names of the rules in the database. */
enum class ProgressionRuleType {
    DOUBLE_PROGRESSION,
    LINEAR,
    STAGES,
    FIVE_THREE_ONE,
    NONE,
}

/**
 * How a planned exercise moves on from session to session (SPEC §7). An increment is in kg; null means
 * the weight step of the exercise.
 */
sealed interface ProgressionRule {
    val type: ProgressionRuleType

    /** A step more weight once every set reaches the top of the rep range; until then a rep more per set. */
    data object DoubleProgression : ProgressionRule {
        override val type: ProgressionRuleType get() = ProgressionRuleType.DOUBLE_PROGRESSION
    }

    /**
     * More weight after every session that made all its reps. After [missesBeforeDeload] missed sessions in
     * a row, [deloadPercent] less.
     */
    data class Linear(
        val incrementKg: Double? = null,
        val missesBeforeDeload: Int = DEFAULT_MISSES,
        val deloadPercent: Int = DEFAULT_DELOAD_PERCENT,
    ) : ProgressionRule {
        override val type: ProgressionRuleType get() = ProgressionRuleType.LINEAR
    }

    /**
     * Linear, but a missed session moves on to other sets and reps with the same weight, such as 5 × 3,
     * then 6 × 2, then 10 × 1 (GZCLP). After the last stage the first one starts again, [resetPercent] lighter.
     */
    data class Stages(
        val stages: List<Stage>,
        val incrementKg: Double? = null,
        val resetPercent: Int = DEFAULT_RESET_PERCENT,
    ) : ProgressionRule {
        override val type: ProgressionRuleType get() = ProgressionRuleType.STAGES
    }

    /**
     * 5/3/1: rounds of four sessions at percentages of a training max, which grows by [incrementKg] after
     * each round. Sessions since [since] count. Without a training max there is nothing to suggest.
     */
    data class FiveThreeOne(
        val trainingMaxKg: Double? = null,
        val incrementKg: Double = DEFAULT_ROUND_INCREMENT_KG,
        val since: Long = 0,
    ) : ProgressionRule {
        override val type: ProgressionRuleType get() = ProgressionRuleType.FIVE_THREE_ONE
    }

    /** Repeats the last session. */
    data object None : ProgressionRule {
        override val type: ProgressionRuleType get() = ProgressionRuleType.NONE
    }

    companion object {
        const val DEFAULT_MISSES = 3
        const val DEFAULT_DELOAD_PERCENT = 10
        const val DEFAULT_RESET_PERCENT = 15
        const val DEFAULT_ROUND_INCREMENT_KG = 2.5
    }
}

/** Sets and reps of one stage, such as 5 × 3. */
data class Stage(val sets: Int, val reps: Int)
