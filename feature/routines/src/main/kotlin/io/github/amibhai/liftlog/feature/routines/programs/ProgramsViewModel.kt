package io.github.amibhai.liftlog.feature.routines.programs

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.program.ProgramTemplate
import io.github.amibhai.liftlog.core.domain.program.ProgramTemplates
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class ProgramsUiState(
    /** Null until loaded. */
    val programs: List<Program>? = null,
    val templates: List<ProgramTemplate> = ProgramTemplates.all,
    /** A program that was just created, for its page to open. */
    val openProgram: ProgramId? = null,
)

/** Your programs, and the built-in ones to start from (SPEC §7). */
@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val programs: ProgramRepository,
    private val ids: IdGenerator,
    private val work: SequentialWork,
) : ViewModel() {

    private val openProgram = MutableStateFlow<ProgramId?>(null)

    val uiState: StateFlow<ProgramsUiState> = combine(programs.observeAll(), openProgram) { all, open ->
        ProgramsUiState(programs = all, openProgram = open)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProgramsUiState())

    /** Creates an empty program and then opens it; its page deletes it again if it stays empty. */
    fun newProgram() {
        val program = Program(ProgramId(ids.newId()), name = "")
        work.submit {
            programs.create(program)
            openProgram.update { program.id }
        }
    }

    fun onProgramOpened() = openProgram.update { null }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
