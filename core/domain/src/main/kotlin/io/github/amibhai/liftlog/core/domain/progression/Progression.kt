package io.github.amibhai.liftlog.core.domain.progression

import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.domain.units.Weight
import kotlin.math.abs

/** How far outside the rep range an unplanned session may be and still count for a plan. */
private const val REP_TOLERANCE = 2

/** Weights closer than this are the same, whatever unit they were typed in. */
private const val SAME_WEIGHT_KG = 0.05

/** What a slot of a routine should aim for after [history], the earlier sessions of its exercise (SPEC §7). */
fun advise(slot: RoutineExercise, history: List<PlannedSession>, loading: Loading): Advice {
    val track = slot.track(history)
    return Advice(
        basis = track.lastOrNull()?.sets.orEmpty(),
        suggestion = when (val rule = slot.progression) {
            ProgressionRule.DoubleProgression -> doubleProgression(slot, track, loading)
            is ProgressionRule.Linear -> linear(slot, rule, track, loading)
            is ProgressionRule.Stages -> staged(rule, track.filter { it.slot == slot.id }, loading)
            is ProgressionRule.FiveThreeOne -> fiveThreeOne(rule, track.filter { it.slot == slot.id }, loading)
            ProgressionRule.None -> null
        },
    )
}

/**
 * The sessions the slot builds on, oldest first: its own, those of slots with the same plan (the squat of
 * both days of a 5×5 program), and sessions without a routine that were in the same rep range.
 */
private fun RoutineExercise.track(history: List<PlannedSession>): List<PlannedSession> = history
    .filter { session ->
        val planned = session.slot == id || session.plan == plan
        session.mainSets.isNotEmpty() && (planned || (session.slot == null && fits(session.mainSets)))
    }
    .sortedBy { it.at }

private fun RoutineExercise.fits(sets: List<LoggedSet>): Boolean {
    val range = repRange ?: return true
    val reps = sets.mapNotNull { it.values.reps }.sorted()
    return reps.isNotEmpty() && reps[reps.size / 2] in range.first - REP_TOLERANCE..range.last + REP_TOLERANCE
}

private val RoutineExercise.repRange: IntRange?
    get() = (repMin ?: repMax)?.let { low -> low..(repMax ?: low) }

private fun doubleProgression(slot: RoutineExercise, track: List<PlannedSession>, loading: Loading): Suggestion? {
    val range = slot.repRange
    val last = track.lastOrNull()?.mainSets.orEmpty().take(slot.targetSets)
    if (range == null || last.isEmpty() || last.any { it.values.reps == null }) return null
    val sets = last.padded(slot.targetSets)
    val isDone = last.size == slot.targetSets && last.all { it.reps >= range.last }
    return when {
        !isDone -> Suggestion(
            sets.map { it.copy(reps = ((it.reps ?: 0) + 1).coerceAtMost(range.last)) },
            SuggestionReason.MoreReps,
        )

        sets.all { slot.exercise.type.canGetHarder(it.weight) } -> Suggestion(
            sets.map { it.copy(weight = loading.harder(it.weight ?: Weight.ZERO), reps = range.first) },
            SuggestionReason.HeavierAfter(range.last),
        )

        else -> Suggestion(sets, SuggestionReason.TopOfRange)
    }
}

private fun linear(
    slot: RoutineExercise,
    rule: ProgressionRule.Linear,
    track: List<PlannedSession>,
    loading: Loading,
): Suggestion? {
    val last = track.lastOrNull()?.mainSets?.map { it.values }.orEmpty()
    val goal = slot.repRange?.first ?: last.mapNotNull { it.reps }.maxOrNull()
    val isLoaded = slot.exercise.type.weightMeaning != WeightMeaning.ASSISTANCE &&
        last.all { slot.exercise.type.canGetHarder(it.weight) }
    if (goal == null || last.isEmpty() || !isLoaded) return null
    val load = track.last().load
    val misses = track.asReversed().takeWhile { it.load.isAbout(load) && !it.made(slot.targetSets, goal) }.size
    // Weights that only go up are a ramp, kept as it is. Otherwise the weight was dropped along the way.
    val base = if (last.isRamp) last else last.map { it.copy(weight = load) }
    val sets = base.take(slot.targetSets).padded(slot.targetSets).map { it.copy(reps = goal) }
    return when {
        misses == 0 -> Suggestion(
            sets.map { it.copy(weight = loading.harder(it.weight ?: Weight.ZERO, rule.incrementKg)) },
            SuggestionReason.HeavierAfter(goal),
        )

        misses < rule.missesBeforeDeload -> Suggestion(sets, SuggestionReason.Retry(misses, rule.missesBeforeDeload))

        else -> Suggestion(
            sets.map { it.copy(weight = loading.lighter(it.weight ?: Weight.ZERO, rule.deloadPercent)) },
            SuggestionReason.Deload(misses, rule.deloadPercent),
        )
    }
}

private fun staged(rule: ProgressionRule.Stages, sessions: List<PlannedSession>, loading: Loading): Suggestion? {
    if (rule.stages.isEmpty()) return null
    var index = 0
    var weight: Weight? = null
    var step = StageStep.FIRST
    for (session in sessions) {
        val load = session.load ?: continue
        val goal = rule.stages[index]
        when {
            session.made(goal.sets, goal.reps) -> {
                weight = loading.harder(load, rule.incrementKg)
                step = StageStep.HEAVIER
            }

            index < rule.stages.lastIndex -> {
                index++
                weight = load
                step = StageStep.AFTER_A_MISS
            }

            else -> {
                index = 0
                weight = loading.lighter(load, rule.resetPercent)
                step = StageStep.STARTED_OVER
            }
        }
    }
    val stage = rule.stages[index]
    return Suggestion(
        sets = List(stage.sets) { SetValues(weight = weight, reps = stage.reps) },
        reason = SuggestionReason.InStage(index + 1, rule.stages.size, step),
    )
}

/** Percentages of the training max and reps, session by session; the last set of a session is all out. */
private val FiveThreeOneSets: List<List<Pair<Int, Int>>> = listOf(
    listOf(65 to 5, 75 to 5, 85 to 5),
    listOf(70 to 3, 80 to 3, 90 to 3),
    listOf(75 to 5, 85 to 3, 95 to 1),
    listOf(40 to 5, 50 to 5, 60 to 5),
)

private fun fiveThreeOne(
    rule: ProgressionRule.FiveThreeOne,
    sessions: List<PlannedSession>,
    loading: Loading,
): Suggestion? {
    val start = rule.trainingMaxKg ?: return null
    val done = sessions.count { it.at >= rule.since }
    val week = done % FiveThreeOneSets.size
    val trainingMax = start + done / FiveThreeOneSets.size * rule.incrementKg
    return Suggestion(
        sets = FiveThreeOneSets[week].map { (percent, reps) ->
            SetValues(weight = loading.part(Weight(trainingMax), percent), reps = reps)
        },
        reason = SuggestionReason.Week(week + 1, trainingMax),
    )
}

/** The heaviest weight of the main sets. */
private val PlannedSession.load: Weight? get() = mainSets.mapNotNull { it.values.weight }.maxOrNull()

private fun Weight?.isAbout(other: Weight?): Boolean =
    if (this == null || other == null) this == other else abs(kg - other.kg) < SAME_WEIGHT_KG

/** All of [sets] sets with [reps] reps, without taking weight off along the way. */
private fun PlannedSession.made(sets: Int, reps: Int): Boolean {
    val main = mainSets.map { it.values }
    return main.size >= sets && main.isRamp && main.take(sets).all { (it.reps ?: 0) >= reps }
}

private val List<SetValues>.isRamp: Boolean
    get() = zipWithNext().all { (first, second) -> (first.weight ?: Weight.ZERO) <= (second.weight ?: Weight.ZERO) }

private val LoggedSet.reps: Int get() = values.reps ?: 0

/** As many sets as planned; missing ones repeat the last. */
@JvmName("paddedSets")
private fun List<LoggedSet>.padded(count: Int): List<SetValues> = map { it.values }.padded(count)

private fun List<SetValues>.padded(count: Int): List<SetValues> = this + List(
    (count - size).coerceAtLeast(0),
) { last() }
