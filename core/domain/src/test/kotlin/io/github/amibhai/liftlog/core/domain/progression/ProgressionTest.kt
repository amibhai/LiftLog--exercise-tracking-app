package io.github.amibhai.liftlog.core.domain.progression

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.ProgressionRuleType
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.Stage
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.info
import io.github.amibhai.liftlog.core.domain.workout.logged
import io.github.amibhai.liftlog.core.domain.workout.values
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProgressionTest {

    private val kg = Loading(step = 2.5, unit = WeightUnit.KG, lightest = Weight(20.0))

    @Nested
    inner class DoubleProgression {

        @Test
        fun `all sets at the top of the range earn a step more and start at the bottom`() {
            val next = suggest(slot(), session(60.0 to 12, 60.0 to 12, 60.0 to 13))

            assertThat(next?.sets).containsExactly(values(62.5, 8), values(62.5, 8), values(62.5, 8)).inOrder()
            assertThat(next?.reason).isEqualTo(SuggestionReason.HeavierAfter(12))
        }

        @Test
        fun `until then every set aims for one rep more, up to the top`() {
            val next = suggest(slot(), session(60.0 to 12, 60.0 to 10, 60.0 to 9))

            assertThat(next?.sets).containsExactly(values(60.0, 12), values(60.0, 11), values(60.0, 10)).inOrder()
            assertThat(next?.reason).isEqualTo(SuggestionReason.MoreReps)
        }

        @Test
        fun `a missing set keeps the weight, however many reps the others had`() {
            val next = suggest(slot(), session(60.0 to 12, 60.0 to 12))

            assertThat(next?.sets).containsExactly(values(60.0, 12), values(60.0, 12), values(60.0, 12))
        }

        @Test
        fun `sets with different weights move on one by one`() {
            assertThat(suggest(slot(), session(80.0 to 9, 75.0 to 8, 70.0 to 8))?.sets)
                .containsExactly(values(80.0, 10), values(75.0, 9), values(70.0, 9)).inOrder()
            assertThat(suggest(slot(), session(80.0 to 12, 75.0 to 12, 70.0 to 12))?.sets)
                .containsExactly(values(82.5, 8), values(77.5, 8), values(72.5, 8)).inOrder()
        }

        @Test
        fun `warm-ups and drop sets do not count`() {
            val session = PlannedSession(
                at = 1,
                sets = listOf(
                    logged(30.0, 5, SetType.WARMUP),
                    logged(60.0, 12),
                    logged(60.0, 12, SetType.FAILURE),
                    logged(60.0, 12),
                    logged(40.0, 6, SetType.DROP),
                ),
                slot = SLOT,
            )

            assertThat(suggest(slot(), session)?.sets?.first()).isEqualTo(values(62.5, 8))
        }

        @Test
        fun `pounds move on in pounds`() {
            val pounds = Loading(step = 5.0, unit = WeightUnit.LB, lightest = Weight.of(45.0, WeightUnit.LB))
            val lifted = Weight.of(135.0, WeightUnit.LB).kg

            val next = suggest(slot(), session(lifted to 12, lifted to 12, lifted to 12), loading = pounds)

            assertThat(next!!.sets.first().weight!!.displayText(WeightUnit.LB)).isEqualTo("140")
        }

        @Test
        fun `bodyweight exercises add weight from nothing`() {
            val next = suggest(
                slot(type = ExerciseType.BODYWEIGHT_ADDED),
                session(null to 12, null to 12, null to 12),
            )

            assertThat(next?.sets?.first()).isEqualTo(values(2.5, 8))
        }

        @Test
        fun `assisted exercises take assistance off, until there is none left`() {
            val assisted = Loading(step = 2.5, unit = WeightUnit.KG, isAssisted = true)
            val slot = slot(type = ExerciseType.BODYWEIGHT_ASSISTED)

            val next = suggest(slot, session(20.0 to 12, 20.0 to 12, 20.0 to 12), loading = assisted)
            assertThat(next?.sets?.first()).isEqualTo(values(17.5, 8))

            val done = suggest(slot, session(0.0 to 12, 0.0 to 12, 0.0 to 12), loading = assisted)
            assertThat(done?.reason).isEqualTo(SuggestionReason.TopOfRange)
            assertThat(done?.sets?.first()).isEqualTo(values(0.0, 12))
        }

        @Test
        fun `without weight the top of the range is kept`() {
            val next = suggest(slot(type = ExerciseType.REPS_ONLY), session(null to 14, null to 12, null to 12))

            assertThat(next?.reason).isEqualTo(SuggestionReason.TopOfRange)
            assertThat(next?.sets?.map { it.reps }).containsExactly(14, 12, 12).inOrder()
        }

        @Test
        fun `there is nothing to suggest without a range, without history or without reps`() {
            assertThat(suggest(slot(reps = null), session(60.0 to 12))).isNull()
            assertThat(suggest(slot())).isNull()
            assertThat(suggest(slot(type = ExerciseType.DURATION), session(null to null))).isNull()
        }
    }

    @Nested
    inner class Linear {

        private val rule = ProgressionRule.Linear()
        private val fives = slot(sets = 3, reps = 5..5, rule = rule)

        @Test
        fun `a session with all its reps adds weight`() {
            val next = suggest(fives, session(100.0 to 5, 100.0 to 5, 100.0 to 6))

            assertThat(next?.sets).containsExactly(values(102.5, 5), values(102.5, 5), values(102.5, 5))
            assertThat(next?.reason).isEqualTo(SuggestionReason.HeavierAfter(5))
        }

        @Test
        fun `the increment can be chosen`() {
            val next = suggest(
                slot(sets = 1, reps = 5..5, rule = rule.copy(incrementKg = 5.0)),
                session(100.0 to 5),
            )

            assertThat(next?.sets).containsExactly(values(105.0, 5))
        }

        @Test
        fun `missed reps repeat the weight`() {
            val next = suggest(fives, session(100.0 to 5, 100.0 to 5, 100.0 to 4))

            assertThat(next?.sets).containsExactly(values(100.0, 5), values(100.0, 5), values(100.0, 5))
            assertThat(next?.reason).isEqualTo(SuggestionReason.Retry(misses = 1, allowed = 3))
        }

        @Test
        fun `three misses in a row deload by ten percent`() {
            val miss = listOf(100.0 to 5, 100.0 to 4, 100.0 to 4)
            val history = (1L..3L).map { session(*miss.toTypedArray(), at = it) }

            val next = suggest(fives, *history.toTypedArray())

            assertThat(next?.sets?.first()).isEqualTo(values(90.0, 5))
            assertThat(next?.reason).isEqualTo(SuggestionReason.Deload(misses = 3, percent = 10))
        }

        @Test
        fun `a success or another weight starts the count again`() {
            val afterSuccess = suggest(
                fives,
                session(97.5 to 4, 97.5 to 4, 97.5 to 4, at = 1),
                session(97.5 to 5, 97.5 to 5, 97.5 to 5, at = 2),
                session(100.0 to 4, 100.0 to 4, 100.0 to 4, at = 3),
            )

            assertThat(afterSuccess?.reason).isEqualTo(SuggestionReason.Retry(misses = 1, allowed = 3))
        }

        @Test
        fun `taking weight off during the session is a miss, and the planned weight stays`() {
            val next = suggest(fives, session(100.0 to 5, 100.0 to 5, 95.0 to 5))

            assertThat(next?.sets).containsExactly(values(100.0, 5), values(100.0, 5), values(100.0, 5))
        }

        @Test
        fun `a ramp of weights keeps its shape`() {
            val next = suggest(fives, session(80.0 to 5, 90.0 to 5, 100.0 to 5))

            assertThat(next?.sets?.map { it.weight?.kg }).containsExactly(82.5, 92.5, 102.5).inOrder()
        }

        @Test
        fun `a deload never goes below the bar`() {
            val light = session(20.0 to 1, 20.0 to 1, 20.0 to 1)

            val next = suggest(slot(sets = 3, reps = 5..5, rule = rule.copy(missesBeforeDeload = 1)), light)

            assertThat(next?.sets?.first()?.weight).isEqualTo(Weight(20.0))
        }
    }

    @Nested
    inner class Stages {

        private val rule = ProgressionRule.Stages(listOf(Stage(5, 3), Stage(6, 2), Stage(10, 1)), incrementKg = 5.0)
        private val slot = slot(sets = 5, reps = 3..3, rule = rule)

        @Test
        fun `the first session asks for the first stage, with the weight left open`() {
            val next = suggest(slot)

            assertThat(next?.sets).containsExactlyElementsIn(List(5) { values(reps = 3) })
            assertThat(next?.reason).isEqualTo(SuggestionReason.InStage(1, 3, StageStep.FIRST))
        }

        @Test
        fun `success adds weight within the stage`() {
            val next = suggest(slot, sets(5, 100.0, 3))

            assertThat(next?.sets).containsExactlyElementsIn(List(5) { values(105.0, 3) })
            assertThat(next?.reason).isEqualTo(SuggestionReason.InStage(1, 3, StageStep.HEAVIER))
        }

        @Test
        fun `a miss moves to the next stage with the same weight`() {
            val next = suggest(slot, sets(5, 100.0, 3, at = 1), sets(5, 105.0, 2, at = 2))

            assertThat(next?.sets).containsExactlyElementsIn(List(6) { values(105.0, 2) })
            assertThat(next?.reason).isEqualTo(SuggestionReason.InStage(2, 3, StageStep.AFTER_A_MISS))
        }

        @Test
        fun `a miss in the last stage starts over, lighter`() {
            val next = suggest(
                slot,
                sets(5, 100.0, 2, at = 1),
                sets(6, 100.0, 1, at = 2),
                sets(9, 100.0, 1, at = 3),
            )

            assertThat(next?.sets).containsExactlyElementsIn(List(5) { values(85.0, 3) })
            assertThat(next?.reason).isEqualTo(SuggestionReason.InStage(1, 3, StageStep.STARTED_OVER))
        }

        @Test
        fun `only the sessions of the slot itself count`() {
            val elsewhere = sets(3, 60.0, 10, at = 5).copy(slot = RoutineExerciseId("other"))

            assertThat(suggest(slot, sets(5, 100.0, 3, at = 1), elsewhere)?.sets?.first()).isEqualTo(values(105.0, 3))
        }
    }

    @Nested
    inner class FiveThreeOne {

        private val rule = ProgressionRule.FiveThreeOne(trainingMaxKg = 100.0, incrementKg = 2.5, since = 10)
        private val slot = slot(sets = 3, reps = 5..5, rule = rule)

        @Test
        fun `the first week is three sets of five at rising percentages`() {
            val next = suggest(slot)

            assertThat(next?.sets).containsExactly(values(65.0, 5), values(75.0, 5), values(85.0, 5)).inOrder()
            assertThat(next?.reason).isEqualTo(SuggestionReason.Week(1, 100.0))
        }

        @Test
        fun `weeks follow each other, whatever was lifted`() {
            val next = suggest(slot, sets(3, 85.0, 3, at = 11), sets(3, 90.0, 3, at = 12))

            assertThat(next?.sets).containsExactly(values(75.0, 5), values(85.0, 3), values(95.0, 1)).inOrder()
            assertThat(next?.reason).isEqualTo(SuggestionReason.Week(3, 100.0))
        }

        @Test
        fun `after four weeks the training max grows`() {
            val done = (11L..14L).map { sets(3, 90.0, 3, at = it) }

            val next = suggest(slot, *done.toTypedArray())

            assertThat(next?.reason).isEqualTo(SuggestionReason.Week(1, 102.5))
            assertThat(next?.sets?.map { it.weight?.kg }).containsExactly(67.5, 77.5, 87.5).inOrder()
        }

        @Test
        fun `sessions from before the training max was set do not count`() {
            assertThat(suggest(slot, sets(3, 85.0, 5, at = 9))?.reason).isEqualTo(SuggestionReason.Week(1, 100.0))
        }

        @Test
        fun `without a training max nothing is suggested`() {
            assertThat(suggest(slot(rule = rule.copy(trainingMaxKg = null)))).isNull()
        }
    }

    @Nested
    inner class WhatItBuildsOn {

        private val other = RoutineExerciseId("other")

        @Test
        fun `slots with the same plan share their progress`() {
            val sameDay = session(60.0 to 12, 60.0 to 12, 60.0 to 12)
                .copy(slot = other, plan = SessionPlan(8, 12, ProgressionRuleType.DOUBLE_PROGRESSION))

            assertThat(suggest(slot(), sameDay)?.sets?.first()).isEqualTo(values(62.5, 8))
        }

        @Test
        fun `a heavy day does not push the light day`() {
            val light = session(60.0 to 10, 60.0 to 10, 60.0 to 10, at = 1)
            val heavy = session(90.0 to 4, 90.0 to 4, at = 2)
                .copy(slot = other, plan = SessionPlan(3, 5, ProgressionRuleType.DOUBLE_PROGRESSION))

            val advice = advise(slot(), listOf(heavy, light), kg)

            assertThat(advice.basis).isEqualTo(light.sets)
            assertThat(advice.suggestion?.sets?.first()).isEqualTo(values(60.0, 11))
        }

        @Test
        fun `a session without a routine counts when its reps fit`() {
            val close = session(65.0 to 9, 65.0 to 8, 65.0 to 8, at = 2).copy(slot = null)
            val farOff = session(90.0 to 3, 90.0 to 3, at = 3).copy(slot = null)

            val advice = advise(slot(), listOf(session(60.0 to 9, 60.0 to 9, 60.0 to 9, at = 1), close, farOff), kg)

            assertThat(advice.basis).isEqualTo(close.sets)
        }

        @Test
        fun `repeating has no suggestion but still builds on the last session`() {
            val advice = advise(slot(rule = ProgressionRule.None), listOf(session(60.0 to 8)), kg)

            assertThat(advice.suggestion).isNull()
            assertThat(advice.basis).hasSize(1)
        }
    }

    private fun suggest(slot: RoutineExercise, vararg history: PlannedSession, loading: Loading = kg): Suggestion? =
        advise(slot, history.toList(), loading).suggestion

    private fun slot(
        sets: Int = 3,
        reps: IntRange? = 8..12,
        rule: ProgressionRule = ProgressionRule.DoubleProgression,
        type: ExerciseType = ExerciseType.WEIGHT_REPS,
    ) = RoutineExercise(
        id = SLOT,
        exercise = info(type = type),
        targetSets = sets,
        repMin = reps?.first,
        repMax = reps?.last,
        progression = rule,
    )

    private fun session(vararg sets: Pair<Double?, Int?>, at: Long = 1): PlannedSession =
        PlannedSession(at = at, sets = sets.map { (weight, reps) -> logged(weight, reps) }, slot = SLOT)

    private fun sets(count: Int, weight: Double, reps: Int, at: Long = 11): PlannedSession =
        PlannedSession(at = at, sets = List<LoggedSet>(count) { logged(weight, reps) }, slot = SLOT)

    private companion object {
        val SLOT = RoutineExerciseId("slot")
    }
}
