package io.github.amibhai.liftlog.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class HomeUiState(val exerciseCount: Int? = null)

@HiltViewModel
class HomeViewModel @Inject constructor(exerciseRepository: ExerciseRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = exerciseRepository.observeStats()
        .map { HomeUiState(exerciseCount = it.totalExercises) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
