package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.progression.Suggestion

/** A set to create, with its ghost values. */
data class PlannedSet(val type: SetType, val target: SetValues)

/**
 * Pre-fills the sets of an exercise from the last time it was done and from its progression rule
 * (SPEC §5, §7), so that a set that goes as planned takes one tap.
 */
object SetPlanner {

    /** An exercise added by hand: the same sets as last time. */
    fun forExercise(previous: List<LoggedSet>): List<PlannedSet> = when {
        previous.isEmpty() -> List(RoutineExercise.DEFAULT_SETS) { PlannedSet(SetType.WORKING, SetValues.EMPTY) }
        else -> previous.map { it.planned() }
    }

    /**
     * An exercise of a routine: last time's warm-ups, then exactly the planned number of working sets, each
     * still followed by the drop sets it had. The [suggestion] decides over the working sets.
     */
    fun forRoutine(
        exercise: RoutineExercise,
        previous: List<LoggedSet>,
        suggestion: Suggestion? = null,
    ): List<PlannedSet> {
        val count = suggestion?.sets?.size ?: exercise.targetSets
        val kept = keepMainSets(previous, count)
        val missing = count - kept.count { it.type.isMain }
        val filler = kept.lastOrNull { it.type.isMain }?.values ?: SetValues(reps = exercise.repMin)
        val sets = kept.map { it.planned() } + List(missing.coerceAtLeast(0)) { PlannedSet(SetType.WORKING, filler) }
        return if (suggestion == null) sets else sets.following(suggestion)
    }

    /** The working sets take the suggested numbers, and keep their own where none is suggested. */
    private fun List<PlannedSet>.following(suggestion: Suggestion): List<PlannedSet> {
        val targets = suggestion.sets.iterator()
        return map { if (it.type.isMain) it.copy(target = targets.next().orElse(it.target)) else it }
    }

    /** Everything up to and including the [count]th main set and its drop sets. */
    private fun keepMainSets(sets: List<LoggedSet>, count: Int): List<LoggedSet> {
        var seen = 0
        return sets.takeWhile { set ->
            if (set.type.isMain) seen++
            seen <= count
        }
    }

    /** Failure describes how a set went, not how the next one is planned. */
    private fun LoggedSet.planned() = PlannedSet(if (type == SetType.FAILURE) SetType.WORKING else type, values)
}
