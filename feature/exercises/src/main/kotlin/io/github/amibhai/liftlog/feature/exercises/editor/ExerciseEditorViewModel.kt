package io.github.amibhai.liftlog.feature.exercises.editor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.feature.exercises.navigation.ExerciseEditorDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ExerciseEditorUiState(
    val draft: ExerciseDraft = ExerciseDraft(),
    val isNew: Boolean = true,
    /** False until an existing exercise has loaded into the form. */
    val isReady: Boolean = false,
    /** Set once saved; the screen closes with it. */
    val savedId: ExerciseId? = null,
)

/** Creates a custom exercise or edits any exercise (SPEC §3). */
@HiltViewModel
class ExerciseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exercises: ExerciseRepository,
    private val editor: ExerciseEditor,
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<ExerciseEditorDestination>()
    private val state = MutableStateFlow(
        ExerciseEditorUiState(
            draft = ExerciseDraft(name = destination.name.orEmpty()),
            isNew = destination.exerciseId == null,
            isReady = destination.exerciseId == null,
        ),
    )

    val uiState: StateFlow<ExerciseEditorUiState> = state.asStateFlow()

    init {
        destination.exerciseId?.let(::load)
    }

    fun change(transform: (ExerciseDraft) -> ExerciseDraft) = state.update { it.copy(draft = transform(it.draft)) }

    /** A muscle is either primary or secondary, so choosing it in one list removes it from the other. */
    fun togglePrimary(muscle: Muscle) = change { draft ->
        draft.copy(
            primaryMuscles = draft.primaryMuscles.toggled(muscle),
            secondaryMuscles = draft.secondaryMuscles - muscle,
        )
    }

    fun toggleSecondary(muscle: Muscle) = change { draft ->
        draft.copy(
            primaryMuscles = draft.primaryMuscles - muscle,
            secondaryMuscles = draft.secondaryMuscles.toggled(muscle),
        )
    }

    fun save() {
        val draft = state.value.draft
        if (!draft.isValid) return
        viewModelScope.launch {
            val id = editor.save(draft)
            state.update { it.copy(savedId = id) }
        }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val detail = exercises.observeDetail(ExerciseId(id)).first()
            state.update { it.copy(draft = detail?.toDraft() ?: it.draft, isReady = true) }
        }
    }

    private fun ExerciseDetail.toDraft() = ExerciseDraft(
        id = id,
        name = name,
        type = type,
        equipment = equipment,
        category = category,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles,
        isUnilateral = isUnilateral,
        instructions = instructions,
    )

    private fun List<Muscle>.toggled(muscle: Muscle) = if (muscle in this) this - muscle else this + muscle
}
