package io.github.amibhai.liftlog.feature.routines.programs.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.ActiveProgram
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.RoutineSummary
import io.github.amibhai.liftlog.core.domain.program.addDay
import io.github.amibhai.liftlog.core.domain.program.nextDay
import io.github.amibhai.liftlog.core.domain.program.removeDay
import io.github.amibhai.liftlog.core.domain.program.renameDay
import io.github.amibhai.liftlog.core.domain.program.reorderDays
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.feature.routines.navigation.ProgramDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface ProgramUiState {
    data object Loading : ProgramUiState

    /** Deleted: the screen closes. */
    data object Gone : ProgramUiState

    @Immutable
    data class Ready(
        /** Blank until it is named. */
        val name: String,
        val isActive: Boolean,
        val days: List<ProgramDayUiModel>,
        /** What a day can be added from. */
        val routines: List<RoutineChoice>,
        /** A day was just removed and can be brought back. */
        val canUndo: Boolean,
    ) : ProgramUiState
}

@Immutable
data class ProgramDayUiModel(
    val id: ProgramDayId,
    val name: String,
    /** Null when the routine was deleted. */
    val routineId: RoutineId?,
    val exercises: List<String>,
    val isNext: Boolean,
)

@Immutable
data class RoutineChoice(val id: RoutineId, val name: String)

sealed interface ProgramAction {
    data class Rename(val name: String) : ProgramAction

    data class Follow(val isActive: Boolean) : ProgramAction

    data class AddDay(val routine: RoutineChoice) : ProgramAction

    data class RenameDay(val id: ProgramDayId, val name: String) : ProgramAction

    data class RemoveDay(val id: ProgramDayId) : ProgramAction

    data class Reorder(val ids: List<ProgramDayId>) : ProgramAction

    data object Delete : ProgramAction

    data object Undo : ProgramAction

    data object DismissMessage : ProgramAction
}

/** One of your programs: its rotation, and whether it is followed (SPEC §7). Every change is stored at once. */
@HiltViewModel
class ProgramViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programs: ProgramRepository,
    private val ids: IdGenerator,
    private val work: SequentialWork,
    routines: RoutineRepository,
) : ViewModel() {

    private val id = ProgramId(savedStateHandle.toRoute<ProgramDestination>().programId)
    private val removed = MutableStateFlow<Program?>(null)

    private class Stored(val program: Program?)

    private val stored: StateFlow<Stored?> = programs.observe(id).map(::Stored)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<ProgramUiState> =
        combine(stored, programs.observeActive(), routines.observeAll(), removed) { stored, active, all, removed ->
            when {
                stored == null -> ProgramUiState.Loading
                stored.program == null -> ProgramUiState.Gone
                else -> ready(stored.program, active, all, canUndo = removed != null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProgramUiState.Loading)

    fun onAction(action: ProgramAction) {
        when (action) {
            is ProgramAction.Rename -> edit { it.copy(name = action.name.trim()) }

            is ProgramAction.Follow -> work.submit { programs.activate(id.takeIf { action.isActive }) }

            is ProgramAction.AddDay -> {
                val day = ProgramDay(ProgramDayId(ids.newId()), action.routine.name, action.routine.id)
                edit { it.addDay(day) }
            }

            is ProgramAction.RenameDay -> edit { it.renameDay(action.id, action.name.trim()) }

            is ProgramAction.RemoveDay -> {
                removed.value = stored.value?.program
                edit { it.removeDay(action.id) }
            }

            is ProgramAction.Reorder -> edit { it.reorderDays(action.ids) }

            ProgramAction.Delete -> work.submit { programs.delete(id) }

            ProgramAction.Undo -> {
                val before = removed.value
                if (before != null) edit { it.copy(days = before.days) }
                removed.update { null }
            }

            ProgramAction.DismissMessage -> removed.update { null }
        }
    }

    private fun edit(change: (Program) -> Program) = work.submit { programs.edit(id, change) }

    /** A new program that was left without a name or days is not worth keeping. */
    override fun onCleared() {
        // In the queue, so that it sees every change made before.
        work.submit {
            val program = programs.observe(id).first()
            if (program != null && program.name.isBlank() && program.days.isEmpty()) programs.delete(id)
        }
    }

    private fun ready(
        program: Program,
        active: ActiveProgram?,
        routines: List<RoutineSummary>,
        canUndo: Boolean,
    ): ProgramUiState.Ready {
        val byId = routines.associate { it.routine.id to it.routine }
        val next = active?.takeIf { it.program.id == id }?.nextDay
        return ProgramUiState.Ready(
            name = program.name,
            isActive = program.isActive,
            days = program.days.map { day ->
                ProgramDayUiModel(
                    id = day.id,
                    name = day.name,
                    routineId = day.routineId,
                    exercises = day.routineId?.let(byId::get)?.exercises.orEmpty().map { it.exercise.name },
                    isNext = day.id == next?.id,
                )
            },
            routines = routines.map { RoutineChoice(it.routine.id, it.routine.name) },
            canUndo = canUndo,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
