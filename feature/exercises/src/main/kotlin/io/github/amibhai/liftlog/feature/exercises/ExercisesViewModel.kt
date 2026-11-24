package io.github.amibhai.liftlog.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseBrowser
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseFilter
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** The Exercises tab: the library with the same suggestions, filters and search as the picker. */
@HiltViewModel
class ExercisesViewModel @Inject constructor(browser: ExerciseBrowser) : ViewModel() {

    private val filter = MutableStateFlow(ExerciseFilter())

    val uiState: StateFlow<ExerciseListUiState> = browser.uiState(filter, current = flowOf(emptySet()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExerciseListUiState())

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }

    fun onMuscleChange(muscle: Muscle?) = filter.update { it.copy(muscle = muscle) }

    fun onEquipmentChange(equipment: Equipment?) = filter.update { it.copy(equipment = equipment) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
