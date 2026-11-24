package io.github.amibhai.liftlog.core.domain.progression

import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.ProgressionRuleType
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetValues

/** A past session of one exercise, with the routine slot that planned it. */
data class PlannedSession(
    val at: Long,
    val sets: List<LoggedSet>,
    /** Null when it was not started from a routine. */
    val slot: RoutineExerciseId? = null,
    val plan: SessionPlan? = null,
) {
    /** Neither warm-ups nor drop sets: the sets a plan is about. */
    val mainSets: List<LoggedSet> get() = sets.filter { it.type.isMain }
}

/** What a routine slot asks for; slots that ask for the same share their progress. */
data class SessionPlan(val repMin: Int?, val repMax: Int?, val rule: ProgressionRuleType)

/** What to do next, one entry per working set, and why. */
data class Suggestion(val sets: List<SetValues>, val reason: SuggestionReason)

sealed interface SuggestionReason {
    /** Every set made [reps] reps. */
    data class HeavierAfter(val reps: Int) : SuggestionReason

    /** The same weight, a rep more where there is room. */
    data object MoreReps : SuggestionReason

    /** Every set is at the top of the rep range, and there is no weight to add. */
    data object TopOfRange : SuggestionReason

    /** Not all reps were made: the same once more. After [allowed] misses in a row comes a deload. */
    data class Retry(val misses: Int, val allowed: Int) : SuggestionReason

    data class Deload(val misses: Int, val percent: Int) : SuggestionReason

    /** Stage [number] of [count], for example 2 of 3. */
    data class InStage(val number: Int, val count: Int, val stage: StageStep) : SuggestionReason

    /** Session [week] of a 5/3/1 round; the fourth is a deload. */
    data class Week(val week: Int, val trainingMaxKg: Double) : SuggestionReason
}

/** How a stage was reached. */
enum class StageStep { FIRST, HEAVIER, AFTER_A_MISS, STARTED_OVER }

/** What a routine slot builds on, and what to do next. */
data class Advice(
    /** The last session of the same plan; empty when there is none. */
    val basis: List<LoggedSet> = emptyList(),
    /** Null when there is nothing to build on, or no rule. */
    val suggestion: Suggestion? = null,
)

internal val RoutineExercise.plan: SessionPlan get() = SessionPlan(repMin, repMax, progression.type)
