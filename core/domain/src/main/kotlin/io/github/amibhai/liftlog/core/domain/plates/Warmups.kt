package io.github.amibhai.liftlog.core.domain.plates

import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WarmupStep
import io.github.amibhai.liftlog.core.domain.progression.Loading
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.workout.barWeight

private const val PERCENT = 100.0

/** The warm-up sets before [working] sets of [exercise], in weights that can be loaded (SPEC §5). */
fun warmupSets(exercise: ExerciseInfo, working: Weight, settings: UserSettings): List<SetValues> {
    val bar = exercise.barWeight(settings)
    val unit = settings.weightUnit
    val loading = Loading.of(exercise, settings)
    return warmupSets(working, settings.warmupSteps, bar) { weight ->
        if (bar == null) loading.nearest(weight) else nearestLoadable(weight, bar, settings.plates(unit), unit)
    }
}

/** Steps that would not be heavier than the one before, or as heavy as the [working] weight, are left out. */
internal fun warmupSets(
    working: Weight,
    steps: List<WarmupStep>,
    bar: Weight?,
    nearest: (Weight) -> Weight,
): List<SetValues> = buildList {
    for (step in steps.sortedBy { it.percent }) {
        val weight = when {
            step.percent > 0 -> maxOf(nearest(Weight(working.kg * step.percent / PERCENT)), bar ?: Weight.ZERO)
            else -> bar ?: Weight.ZERO
        }
        val heaviest = lastOrNull()?.weight ?: Weight.ZERO
        if (weight > heaviest && weight < working) add(SetValues(weight = weight, reps = step.reps))
    }
}
