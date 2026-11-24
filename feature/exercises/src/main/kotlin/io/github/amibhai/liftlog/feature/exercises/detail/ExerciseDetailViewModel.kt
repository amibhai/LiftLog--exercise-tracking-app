package io.github.amibhai.liftlog.feature.exercises.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.feature.exercises.navigation.ExerciseDetailDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState

    data object NotFound : ExerciseDetailUiState

    @Immutable
    data class Loaded(val exercise: ExerciseDetail) : ExerciseDetailUiState
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle, repository: ExerciseRepository) :
    ViewModel() {
    private val exerciseId = ExerciseId(savedStateHandle.toRoute<ExerciseDetailDestination>().exerciseId)

    val uiState: StateFlow<ExerciseDetailUiState> = repository.observeDetail(exerciseId)
        .map { detail -> if (detail == null) ExerciseDetailUiState.NotFound else ExerciseDetailUiState.Loaded(detail) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExerciseDetailUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
