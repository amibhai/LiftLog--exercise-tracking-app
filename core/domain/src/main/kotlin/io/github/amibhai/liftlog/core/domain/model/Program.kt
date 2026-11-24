package io.github.amibhai.liftlog.core.domain.model

/** A rotation of routines (SPEC §7): after the last day comes the first one again. */
data class Program(
    val id: ProgramId,
    val name: String,
    /** The built-in template it was made from; null for one of your own. */
    val templateKey: String? = null,
    val isActive: Boolean = false,
    val days: List<ProgramDay> = emptyList(),
)

/** One day of a program; without a routine when that was deleted. */
data class ProgramDay(val id: ProgramDayId, val name: String, val routineId: RoutineId?)

/** The program that is followed, and the day of it that was done last. */
data class ActiveProgram(val program: Program, val lastDay: ProgramDayId?)
