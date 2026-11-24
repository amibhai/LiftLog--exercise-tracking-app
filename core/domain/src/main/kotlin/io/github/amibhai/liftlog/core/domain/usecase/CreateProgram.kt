package io.github.amibhai.liftlog.core.domain.usecase

import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.program.ProgramTemplate
import io.github.amibhai.liftlog.core.domain.program.TemplateDayName
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.units.Weight
import kotlinx.coroutines.flow.first
import java.time.Clock

/** Copies a built-in program into routines of your own, and follows it from now on (SPEC §7). */
class CreateProgram(
    private val programs: ProgramRepository,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    /** [name] and [dayNames] are in the language of the user. Lifts without a training max can get one later. */
    suspend operator fun invoke(
        template: ProgramTemplate,
        name: String,
        dayNames: Map<TemplateDayName, String>,
        trainingMaxes: Map<ExerciseId, Weight> = emptyMap(),
    ): ProgramId {
        val unit = settings.settings.first().weightUnit
        val library = exercises.infos(template.exercises.toList()).associateBy { it.id }
        val now = clock.millis()
        val days = template.days.map { day ->
            val routine = Routine(
                id = RoutineId(ids.newId()),
                name = dayNames[day.name].orEmpty(),
                exercises = day.slots.mapNotNull { slot ->
                    library[slot.exercise]?.let { exercise ->
                        RoutineExercise(
                            id = RoutineExerciseId(ids.newId()),
                            exercise = exercise,
                            targetSets = slot.sets,
                            repMin = slot.reps?.first,
                            repMax = slot.reps?.last,
                            restSeconds = slot.restSeconds,
                            progression = slot.rule(unit, trainingMaxes[slot.exercise], now),
                        )
                    }
                },
            )
            routines.create(routine)
            ProgramDay(ProgramDayId(ids.newId()), routine.name, routine.id)
        }
        val program = Program(ProgramId(ids.newId()), name, template.key.name, days = days)
        programs.create(program)
        programs.activate(program.id)
        return program.id
    }
}
