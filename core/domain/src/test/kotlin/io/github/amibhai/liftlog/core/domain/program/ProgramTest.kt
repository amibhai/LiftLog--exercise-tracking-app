package io.github.amibhai.liftlog.core.domain.program

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.amibhai.liftlog.core.domain.model.ActiveProgram
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.Stage
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.jupiter.api.Test

class ProgramTest {

    private val program = Program(
        ProgramId("ppl"),
        "PPL",
        days = listOf(day("push"), day("pull"), day("legs")),
    )

    @Test
    fun `a program begins with its first day`() {
        assertThat(active(last = null).nextDay?.name).isEqualTo("push")
    }

    @Test
    fun `the next day follows the last one and wraps around`() {
        assertThat(active("push").nextDay?.name).isEqualTo("pull")
        assertThat(active("legs").nextDay?.name).isEqualTo("push")
    }

    @Test
    fun `days whose routine was deleted are skipped`() {
        val gap = program.copy(days = listOf(day("push"), ProgramDay(ProgramDayId("pull"), "pull", null), day("legs")))

        assertThat(ActiveProgram(gap, ProgramDayId("push")).nextDay?.name).isEqualTo("legs")
        assertThat(ActiveProgram(gap.copy(days = emptyList()), null).nextDay).isNull()
    }

    @Test
    fun `a day that was removed since starts the rotation again`() {
        assertThat(active("gone").nextDay?.name).isEqualTo("push")
    }

    @Test
    fun `a routine counts for the next time it comes up`() {
        val twice = program.copy(days = listOf(day("a", "upper"), day("b", "lower"), day("c", "upper")))

        assertThat(ActiveProgram(twice, ProgramDayId("a")).dayOf(RoutineId("upper"))?.name).isEqualTo("c")
        assertThat(ActiveProgram(twice, ProgramDayId("c")).dayOf(RoutineId("upper"))?.name).isEqualTo("a")
        assertThat(ActiveProgram(twice, null).dayOf(RoutineId("other"))).isNull()
    }

    @Test
    fun `days can be added, removed, renamed and reordered`() {
        val edited = program
            .addDay(day("arms"))
            .removeDay(ProgramDayId("pull"))
            .renameDay(ProgramDayId("legs"), "Lower body")
            .reorderDays(listOf(ProgramDayId("arms"), ProgramDayId("push")))

        assertThat(edited.days.map { it.name }).containsExactly("arms", "push", "Lower body").inOrder()
    }

    @Test
    fun `there is a template for every key and every plan makes sense`() {
        assertThat(ProgramTemplates.all.map { it.key }).containsExactlyElementsIn(TemplateKey.entries)
        for (template in ProgramTemplates.all) {
            assertThat(template.days.size).isAtLeast(2)
            assertThat(template.days.map { it.name }).containsNoDuplicates()
            for (day in template.days) {
                assertWithMessage("${template.key} ${day.name}")
                    .that(day.slots.map { it.exercise }).containsNoDuplicates()
                assertThat(day.slots.all { it.sets in 1..6 && it.reps!!.first in 3..15 }).isTrue()
            }
        }
    }

    @Test
    fun `templates progress in the units of the plates`() {
        val template = ProgramTemplates.of(TemplateKey.FIVE_BY_FIVE)
        val deadlift = template.days.last().slots.last()
        val bench = template.days.first().slots[1]

        assertThat(bench.rule(WeightUnit.KG, null, NOW)).isEqualTo(ProgressionRule.Linear())
        assertThat(deadlift.rule(WeightUnit.KG, null, NOW)).isEqualTo(ProgressionRule.Linear(incrementKg = 5.0))
        val pounds = deadlift.rule(WeightUnit.LB, null, NOW) as ProgressionRule.Linear
        assertThat(Weight(pounds.incrementKg!!).displayText(WeightUnit.LB)).isEqualTo("10")
    }

    @Test
    fun `gzclp moves through its stages`() {
        val (first, second) = ProgramTemplates.of(TemplateKey.GZCLP).days.first().slots

        val tier1 = first.rule(WeightUnit.KG, null, NOW) as ProgressionRule.Stages
        val tier2 = second.rule(WeightUnit.KG, null, NOW) as ProgressionRule.Stages
        assertThat(tier1.stages).containsExactly(Stage(5, 3), Stage(6, 2), Stage(10, 1)).inOrder()
        assertThat(tier2.stages).containsExactly(Stage(3, 10), Stage(3, 8), Stage(3, 6)).inOrder()
        assertThat(tier1.incrementKg).isEqualTo(5.0)
        assertThat(tier2.incrementKg).isNull()
    }

    @Test
    fun `five three one asks for the training max of its four lifts`() {
        val template = ProgramTemplates.of(TemplateKey.FIVE_THREE_ONE)

        assertThat(template.trainingMaxLifts).hasSize(4)
        val press = template.days.first().slots.first()
        assertThat(press.rule(WeightUnit.KG, Weight(50.0), NOW))
            .isEqualTo(ProgressionRule.FiveThreeOne(trainingMaxKg = 50.0, incrementKg = 2.5, since = NOW))
        assertThat(ProgramTemplates.of(TemplateKey.UPPER_LOWER).trainingMaxLifts).isEmpty()
    }

    private fun active(last: String?) = ActiveProgram(program, last?.let(::ProgramDayId))

    private fun day(id: String, routine: String = id) = ProgramDay(ProgramDayId(id), id, RoutineId(routine))

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
