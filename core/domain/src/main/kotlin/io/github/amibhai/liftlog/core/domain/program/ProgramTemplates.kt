package io.github.amibhai.liftlog.core.domain.program

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Stage

private const val LONG_REST = 180
private const val MEDIUM_REST = 120
private const val SHORT_REST = 90

/** Stages of GZCLP, where a missed session moves on to the next one. */
internal val GzclpTier1: List<Stage> = stages("5 x 3", "6 x 2", "10 x 1")
internal val GzclpTier2: List<Stage> = stages("3 x 10", "3 x 8", "3 x 6")

/** The exercises of the built-in programs, by their stable library ids. */
private object Lift {
    val bench = ExerciseId("545af18a-d600-50a2-b710-e6f7abbb691f")
    val squat = ExerciseId("2ee1d0bd-d6ae-52b6-aee5-e25505042a47")
    val deadlift = ExerciseId("ee09b803-450a-5f70-b9c0-87c04b2b3dfe")
    val press = ExerciseId("23fad1d6-d715-5c33-92fe-76fc68f8cbe9")
    val row = ExerciseId("9e0d7828-a265-5bb1-a146-20d5a7ca1994")
    val pullUp = ExerciseId("4d7d98bd-73a5-5d12-becf-19185e83bed3")
    val latPulldown = ExerciseId("47a72219-844e-52a2-9809-73c032d000da")
    val romanianDeadlift = ExerciseId("fd5f28b3-4f53-5ba2-a636-6cb84e825658")
    val inclineDumbbellPress = ExerciseId("a88fc710-db8c-548c-930e-bc94df960ed8")
    val legPress = ExerciseId("97ce485b-d264-57fb-9033-2d527a1980d5")
    val hipThrust = ExerciseId("7ca24a4f-c126-5c03-b724-8c1247c8f789")
    val cableRow = ExerciseId("cffa8b66-49ee-54a0-8b81-511d6d7f53e1")
    val dumbbellPress = ExerciseId("66fb6b47-00bb-55fd-b06f-3c0c80f9f1b8")
    val dip = ExerciseId("f0992091-c9a3-5c1e-b6c0-d4ae21257fb1")
    val barbellCurl = ExerciseId("7584df4d-07e6-55ca-bc71-7ed8343a8bbe")
    val hammerCurl = ExerciseId("b2ef750a-327a-5dc4-9968-ab86527978cb")
    val pushdown = ExerciseId("321ee924-7969-55cb-aa32-cf867190299c")
    val lateralRaise = ExerciseId("1c9a52ba-9136-5a3b-8705-98d227752be1")
    val facePull = ExerciseId("12e5a9b7-5ef3-5591-bdc4-5c55caa56180")
    val legExtension = ExerciseId("a075ef9f-296b-5cac-aa8e-2468ff855788")
    val legCurl = ExerciseId("1a5a0b68-29e9-5f7a-b6bc-80b93112dc69")
    val standingCalfRaise = ExerciseId("cd5d5f82-8e69-5c54-8bcb-825b33a5020e")
    val seatedCalfRaise = ExerciseId("f4951122-cf22-5560-9ac9-7bd6161eb43e")
    val frontSquat = ExerciseId("dadbb701-a46a-563d-b2a1-451fe7588104")
    val dumbbellRow = ExerciseId("ee82f75f-9266-58b8-8cf8-4a14c609d75c")
    val splitSquat = ExerciseId("9447bf12-50f5-58f0-9c1e-a2d23f5bd307")
    val skullcrusher = ExerciseId("d569a9c5-d3f2-5cd7-a61e-55ba9346be42")
    val legRaise = ExerciseId("ee7853d6-0e1f-5c74-911a-30280840a797")
    val cableCrunch = ExerciseId("0f5cd0a8-441a-5e36-93de-0fc3571469c8")
    val backExtension = ExerciseId("2a2e640c-6ed3-5133-91dc-25ef2b2c12f5")
}

/** The built-in programs (SPEC §7). Everything in them can be changed once they are copied. */
object ProgramTemplates {

    val all: List<ProgramTemplate> =
        listOf(pushPullLegs(), upperLower(), fullBody(), fiveByFive(), gzclp(), fiveThreeOne())

    fun of(key: TemplateKey): ProgramTemplate = all.first { it.key == key }
}

private fun pushPullLegs() = ProgramTemplate(
    TemplateKey.PUSH_PULL_LEGS,
    listOf(
        day(
            TemplateDayName.PUSH,
            main(Lift.bench, "3 x 6-8"),
            main(Lift.dumbbellPress, "3 x 8-10"),
            accessory(Lift.inclineDumbbellPress, "3 x 8-12"),
            accessory(Lift.lateralRaise, "3 x 12-15"),
            accessory(Lift.pushdown, "3 x 10-15"),
        ),
        day(
            TemplateDayName.PULL,
            main(Lift.row, "3 x 6-8"),
            main(Lift.latPulldown, "3 x 8-12"),
            accessory(Lift.cableRow, "3 x 8-12"),
            accessory(Lift.facePull, "3 x 12-15"),
            accessory(Lift.barbellCurl, "3 x 8-12"),
        ),
        day(
            TemplateDayName.LEGS,
            main(Lift.squat, "3 x 6-8"),
            main(Lift.romanianDeadlift, "3 x 8-10"),
            accessory(Lift.legPress, "3 x 10-12"),
            accessory(Lift.legCurl, "3 x 10-15"),
            accessory(Lift.standingCalfRaise, "4 x 10-15"),
        ),
    ),
)

private fun upperLower() = ProgramTemplate(
    TemplateKey.UPPER_LOWER,
    listOf(
        day(
            TemplateDayName.UPPER_A,
            main(Lift.bench, "3 x 5-8"),
            main(Lift.row, "3 x 5-8"),
            accessory(Lift.press, "3 x 6-10"),
            accessory(Lift.latPulldown, "3 x 8-12"),
            accessory(Lift.barbellCurl, "2 x 8-12"),
            accessory(Lift.pushdown, "2 x 10-15"),
        ),
        day(
            TemplateDayName.LOWER_A,
            main(Lift.squat, "3 x 5-8"),
            main(Lift.romanianDeadlift, "3 x 8-10"),
            accessory(Lift.legPress, "3 x 10-12"),
            accessory(Lift.legCurl, "3 x 10-15"),
            accessory(Lift.standingCalfRaise, "3 x 10-15"),
        ),
        day(
            TemplateDayName.UPPER_B,
            main(Lift.inclineDumbbellPress, "3 x 8-12"),
            main(Lift.cableRow, "3 x 8-12"),
            accessory(Lift.dumbbellPress, "3 x 8-12"),
            accessory(Lift.pullUp, "3 x 6-10"),
            accessory(Lift.lateralRaise, "3 x 12-15"),
            accessory(Lift.hammerCurl, "2 x 10-12"),
            accessory(Lift.skullcrusher, "2 x 10-12"),
        ),
        day(
            TemplateDayName.LOWER_B,
            main(Lift.deadlift, "3 x 4-6"),
            main(Lift.frontSquat, "3 x 6-10"),
            accessory(Lift.splitSquat, "3 x 8-12"),
            accessory(Lift.legExtension, "3 x 10-15"),
            accessory(Lift.seatedCalfRaise, "3 x 12-15"),
        ),
    ),
)

private fun fullBody() = ProgramTemplate(
    TemplateKey.FULL_BODY,
    listOf(
        day(
            TemplateDayName.FULL_BODY_A,
            main(Lift.squat, "3 x 5-8"),
            main(Lift.bench, "3 x 5-8"),
            main(Lift.row, "3 x 6-10"),
            accessory(Lift.lateralRaise, "2 x 12-15"),
            accessory(Lift.barbellCurl, "2 x 8-12"),
        ),
        day(
            TemplateDayName.FULL_BODY_B,
            main(Lift.deadlift, "3 x 4-6"),
            main(Lift.press, "3 x 5-8"),
            main(Lift.latPulldown, "3 x 8-12"),
            accessory(Lift.legPress, "2 x 10-12"),
            accessory(Lift.pushdown, "2 x 10-15"),
        ),
        day(
            TemplateDayName.FULL_BODY_C,
            main(Lift.frontSquat, "3 x 6-10"),
            main(Lift.inclineDumbbellPress, "3 x 8-12"),
            main(Lift.cableRow, "3 x 8-12"),
            accessory(Lift.hipThrust, "3 x 8-12"),
            accessory(Lift.facePull, "2 x 12-15"),
            accessory(Lift.standingCalfRaise, "2 x 10-15"),
        ),
    ),
)

private fun fiveByFive() = ProgramTemplate(
    TemplateKey.FIVE_BY_FIVE,
    listOf(
        day(
            TemplateDayName.WORKOUT_A,
            slot(Lift.squat, "5 x 5", TemplateScheme.LINEAR, LONG_REST),
            slot(Lift.bench, "5 x 5", TemplateScheme.LINEAR, LONG_REST),
            slot(Lift.row, "5 x 5", TemplateScheme.LINEAR, LONG_REST),
        ),
        day(
            TemplateDayName.WORKOUT_B,
            slot(Lift.squat, "5 x 5", TemplateScheme.LINEAR, LONG_REST),
            slot(Lift.press, "5 x 5", TemplateScheme.LINEAR, LONG_REST),
            slot(Lift.deadlift, "1 x 5", TemplateScheme.LINEAR, LONG_REST, heavySteps = true),
        ),
    ),
)

private fun gzclp() = ProgramTemplate(
    TemplateKey.GZCLP,
    listOf(
        gzclpDay(TemplateDayName.WORKOUT_A, first = Lift.squat, second = Lift.bench, third = Lift.latPulldown),
        gzclpDay(TemplateDayName.WORKOUT_B, first = Lift.press, second = Lift.deadlift, third = Lift.dumbbellRow),
        gzclpDay(TemplateDayName.WORKOUT_C, first = Lift.bench, second = Lift.squat, third = Lift.latPulldown),
        gzclpDay(TemplateDayName.WORKOUT_D, first = Lift.deadlift, second = Lift.press, third = Lift.dumbbellRow),
    ),
)

private fun gzclpDay(name: TemplateDayName, first: ExerciseId, second: ExerciseId, third: ExerciseId) = day(
    name,
    slot(first, "5 x 3", TemplateScheme.GZCLP_T1, LONG_REST, heavySteps = first in LegLifts),
    slot(second, "3 x 10", TemplateScheme.GZCLP_T2, MEDIUM_REST, heavySteps = second in LegLifts),
    accessory(third, "3 x 15-25"),
)

private fun fiveThreeOne() = ProgramTemplate(
    TemplateKey.FIVE_THREE_ONE,
    listOf(
        fiveThreeOneDay(
            TemplateDayName.PRESS,
            Lift.press,
            accessory(Lift.dip, "3 x 8-12"),
            accessory(Lift.pullUp, "3 x 6-10"),
            accessory(Lift.lateralRaise, "3 x 12-15"),
        ),
        fiveThreeOneDay(
            TemplateDayName.DEADLIFT,
            Lift.deadlift,
            accessory(Lift.legPress, "3 x 10-12"),
            accessory(Lift.backExtension, "3 x 10-15"),
            accessory(Lift.legRaise, "3 x 8-15"),
        ),
        fiveThreeOneDay(
            TemplateDayName.BENCH,
            Lift.bench,
            accessory(Lift.dumbbellRow, "3 x 8-12"),
            accessory(Lift.inclineDumbbellPress, "3 x 8-12"),
            accessory(Lift.barbellCurl, "3 x 8-12"),
        ),
        fiveThreeOneDay(
            TemplateDayName.SQUAT,
            Lift.squat,
            accessory(Lift.romanianDeadlift, "3 x 8-10"),
            accessory(Lift.legCurl, "3 x 10-15"),
            accessory(Lift.cableCrunch, "3 x 10-15"),
        ),
    ),
)

private fun fiveThreeOneDay(name: TemplateDayName, lift: ExerciseId, vararg assistance: TemplateSlot): TemplateDay {
    val main = slot(lift, "3 x 5", TemplateScheme.FIVE_THREE_ONE, LONG_REST, heavySteps = lift in LegLifts)
    return TemplateDay(name, listOf(main) + assistance)
}

private val LegLifts = setOf(Lift.squat, Lift.deadlift)

private fun day(name: TemplateDayName, vararg slots: TemplateSlot) = TemplateDay(name, slots.toList())

private fun main(exercise: ExerciseId, plan: String) = slot(exercise, plan, TemplateScheme.DOUBLE, MEDIUM_REST)

private fun accessory(exercise: ExerciseId, plan: String) = slot(exercise, plan, TemplateScheme.DOUBLE, SHORT_REST)

/** [plan] is sets x reps: "3 x 8-12" or "5 x 5". */
private fun slot(
    exercise: ExerciseId,
    plan: String,
    scheme: TemplateScheme,
    restSeconds: Int,
    heavySteps: Boolean = false,
): TemplateSlot {
    val (sets, reps) = plan.split(" x ")
    val ends = reps.split('-').map { it.toInt() }
    return TemplateSlot(exercise, sets.toInt(), ends.first()..ends.last(), scheme, heavySteps, restSeconds)
}

private fun stages(vararg plans: String): List<Stage> = plans.map { plan ->
    val (sets, reps) = plan.split(" x ")
    Stage(sets.toInt(), reps.toInt())
}
