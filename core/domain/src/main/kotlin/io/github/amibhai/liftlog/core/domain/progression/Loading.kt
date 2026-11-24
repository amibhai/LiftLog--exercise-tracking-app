package io.github.amibhai.liftlog.core.domain.progression

import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.barWeight
import io.github.amibhai.liftlog.core.domain.workout.weightStep
import java.math.BigDecimal
import kotlin.math.roundToInt

private const val PERCENT = 100.0

/** Weights an exercise can be loaded with: whole steps of [step] in [unit], and not less than [lightest]. */
data class Loading(
    val step: Double,
    val unit: WeightUnit,
    val lightest: Weight = Weight.ZERO,
    /** More assistance makes an exercise easier, not harder. */
    val isAssisted: Boolean = false,
) {
    /** One notch harder: by [incrementKg], or by a step. */
    fun harder(weight: Weight, incrementKg: Double? = null): Weight {
        val increment = incrementKg?.let { Weight(it).displayValue(unit).toDouble() } ?: step
        return weight.stepped(if (isAssisted) -increment else increment, unit)
    }

    /** About [percent] less, in whole steps but at least one. */
    fun lighter(weight: Weight, percent: Int): Weight {
        val shown = weight.displayValue(unit).toDouble()
        val steps = (shown * percent / PERCENT / step).roundToInt().coerceAtLeast(1)
        val change = BigDecimal.valueOf(step).multiply(BigDecimal(steps)).toDouble()
        return maxOf(weight.stepped(-change, unit), lightest)
    }

    /** [percent] of [weight], rounded to whole steps. */
    fun part(weight: Weight, percent: Int): Weight = nearest(Weight(weight.kg * percent / PERCENT))

    fun nearest(weight: Weight): Weight {
        val steps = (weight.valueIn(unit) / step).roundToInt()
        return maxOf(Weight.of(BigDecimal.valueOf(step).multiply(BigDecimal(steps)), unit), lightest)
    }

    companion object {
        fun of(exercise: ExerciseInfo, settings: UserSettings) = Loading(
            step = exercise.weightStep(settings),
            unit = settings.weightUnit,
            lightest = exercise.barWeight(settings) ?: Weight.ZERO,
            isAssisted = exercise.type.weightMeaning == WeightMeaning.ASSISTANCE,
        )
    }
}

/** Whether more or less weight is a way to progress, starting from [weight]. */
internal fun ExerciseType.canGetHarder(weight: Weight?): Boolean = when (weightMeaning) {
    WeightMeaning.NONE -> false
    WeightMeaning.LOAD -> weight != null
    WeightMeaning.ADDED -> true
    WeightMeaning.ASSISTANCE -> weight != null && weight > Weight.ZERO
}
