package io.github.amibhai.liftlog.core.domain.usecase

import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.progression.Advice
import io.github.amibhai.liftlog.core.domain.progression.Loading
import io.github.amibhai.liftlog.core.domain.progression.advise
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** What the exercises of a routine should aim for next (SPEC §7). */
class ProgressionAdvisor(private val progress: ProgressRepository, private val settings: SettingsRepository) {

    /** Only sessions from before [before] count. */
    suspend fun advise(slots: List<RoutineExercise>, before: Long): Map<RoutineExerciseId, Advice> {
        if (slots.isEmpty()) return emptyMap()
        val current = settings.settings.first()
        val sessions = progress.plannedSessions(before, slots.mapTo(mutableSetOf()) { it.exercise.id })
        return slots.associate { slot ->
            slot.id to advise(slot, sessions[slot.exercise.id].orEmpty(), Loading.of(slot.exercise, current))
        }
    }
}
