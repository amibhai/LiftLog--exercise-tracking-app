package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues

/** A set to create, with its ghost values. */
data class PlannedSet(val type: SetType, val target: SetValues)

/**
 * Pre-fills the sets of an exercise from the last time it was done (SPEC §5), so that repeating it
 * takes one tap per set. Progression rules build on this in Phase 3.
 */
object SetPlanner {

    /** An exercise added by hand: the same sets as last time. */
    fun forExercise(previous: List<LoggedSet>): List<PlannedSet> = when {
        previous.isEmpty() -> List(RoutineExercise.DEFAULT_SETS) { PlannedSet(SetType.WORKING, SetValues.EMPTY) }
        else -> previous.map { it.planned() }
    }

    /**
     * An exercise of a routine: last time's warm-ups, then exactly the routine's number of working sets,
     * each still followed by the drop sets it had.
     */
    fun forRoutine(exercise: RoutineExercise, previous: List<LoggedSet>): List<PlannedSet> {
        val kept = keepMainSets(previous, exercise.targetSets)
        val missing = exercise.targetSets - kept.count { it.isMain }
        val filler = kept.lastOrNull { it.isMain }?.values ?: SetValues(reps = exercise.repMin)
        return kept.map { it.planned() } + List(missing.coerceAtLeast(0)) { PlannedSet(SetType.WORKING, filler) }
    }

    /** Everything up to and including the [count]th main set and its drop sets. */
    private fun keepMainSets(sets: List<LoggedSet>, count: Int): List<LoggedSet> {
        var seen = 0
        return sets.takeWhile { set ->
            if (set.isMain) seen++
            seen <= count
        }
    }

    private val LoggedSet.isMain get() = type == SetType.WORKING || type == SetType.FAILURE

    /** Failure describes how a set went, not how the next one is planned. */
    private fun LoggedSet.planned() = PlannedSet(if (type == SetType.FAILURE) SetType.WORKING else type, values)
}
