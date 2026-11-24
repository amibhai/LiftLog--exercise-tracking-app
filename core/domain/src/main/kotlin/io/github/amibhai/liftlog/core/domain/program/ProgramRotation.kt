package io.github.amibhai.liftlog.core.domain.program

import io.github.amibhai.liftlog.core.domain.model.ActiveProgram
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.RoutineId

/** The day to do next: the one after the last one, or the first one. Days without a routine are skipped. */
val ActiveProgram.nextDay: ProgramDay? get() = upcoming.firstOrNull { it.routineId != null }

/** The day that doing [routine] counts for: the next time it comes up in the rotation. */
fun ActiveProgram.dayOf(routine: RoutineId): ProgramDay? = upcoming.firstOrNull { it.routineId == routine }

/** All days, beginning with the one after the last one. */
private val ActiveProgram.upcoming: List<ProgramDay>
    get() {
        val next = program.days.indexOfFirst { it.id == lastDay } + 1
        return program.days.drop(next) + program.days.take(next)
    }

fun Program.addDay(day: ProgramDay): Program = copy(days = days + day)

fun Program.removeDay(id: ProgramDayId): Program = copy(days = days.filterNot { it.id == id })

fun Program.renameDay(id: ProgramDayId, name: String): Program =
    copy(days = days.map { if (it.id == id) it.copy(name = name) else it })

/** The days in the order of [ids]; days that are not listed stay at the end. */
fun Program.reorderDays(ids: List<ProgramDayId>): Program {
    val order = ids.withIndex().associate { (index, id) -> id to index }
    return copy(days = days.sortedBy { order[it.id] ?: Int.MAX_VALUE })
}
