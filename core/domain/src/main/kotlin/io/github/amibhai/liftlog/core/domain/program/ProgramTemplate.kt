package io.github.amibhai.liftlog.core.domain.program

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit

/** The built-in programs (SPEC §7). The user interface has a name and a description for each. */
enum class TemplateKey { PUSH_PULL_LEGS, UPPER_LOWER, FULL_BODY, FIVE_BY_FIVE, GZCLP, FIVE_THREE_ONE }

/** The days of the built-in programs, which the user interface names. */
enum class TemplateDayName {
    PUSH,
    PULL,
    LEGS,
    UPPER_A,
    LOWER_A,
    UPPER_B,
    LOWER_B,
    FULL_BODY_A,
    FULL_BODY_B,
    FULL_BODY_C,
    WORKOUT_A,
    WORKOUT_B,
    WORKOUT_C,
    WORKOUT_D,
    PRESS,
    DEADLIFT,
    BENCH,
    SQUAT,
}

/** A program to copy into editable routines. */
data class ProgramTemplate(val key: TemplateKey, val days: List<TemplateDay>) {
    val exercises: Set<ExerciseId> get() = days.flatMapTo(mutableSetOf()) { day -> day.slots.map { it.exercise } }

    /** The lifts that need a training max before the program can start. */
    val trainingMaxLifts: List<ExerciseId>
        get() = days.flatMap { it.slots }.filter { it.scheme == TemplateScheme.FIVE_THREE_ONE }.map { it.exercise }
}

data class TemplateDay(val name: TemplateDayName, val slots: List<TemplateSlot>)

/** One exercise of a day. [heavySteps] progress by two weight steps at a time, as the big lifts for the legs do. */
data class TemplateSlot(
    val exercise: ExerciseId,
    val sets: Int,
    val reps: IntRange?,
    val scheme: TemplateScheme,
    val heavySteps: Boolean,
    val restSeconds: Int?,
) {
    fun rule(unit: WeightUnit, trainingMax: Weight?, now: Long): ProgressionRule {
        val increment = unit.upperIncrement * if (heavySteps) 2 else 1
        val incrementKg = Weight.of(increment, unit).kg
        // A single step is left to the exercise, which can have a step of its own.
        val ownOrHeavy = incrementKg.takeIf { heavySteps }
        return when (scheme) {
            TemplateScheme.DOUBLE -> ProgressionRule.DoubleProgression
            TemplateScheme.LINEAR -> ProgressionRule.Linear(incrementKg = ownOrHeavy)
            TemplateScheme.GZCLP_T1 -> ProgressionRule.Stages(GzclpTier1, incrementKg = ownOrHeavy)
            TemplateScheme.GZCLP_T2 -> ProgressionRule.Stages(GzclpTier2, incrementKg = ownOrHeavy)
            TemplateScheme.FIVE_THREE_ONE -> ProgressionRule.FiveThreeOne(trainingMax?.kg, incrementKg, since = now)
        }
    }
}

enum class TemplateScheme { DOUBLE, LINEAR, GZCLP_T1, GZCLP_T2, FIVE_THREE_ONE }

/** What upper-body lifts usually add: 2.5 kg or 5 lb. */
private val WeightUnit.upperIncrement: Double
    get() = when (this) {
        WeightUnit.KG -> UPPER_INCREMENT_KG
        WeightUnit.LB -> UPPER_INCREMENT_LB
    }

private const val UPPER_INCREMENT_KG = 2.5
private const val UPPER_INCREMENT_LB = 5.0
