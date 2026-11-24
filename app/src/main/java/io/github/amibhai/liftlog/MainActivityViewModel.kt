package io.github.amibhai.liftlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val settings: UserSettings) : MainUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    /** Eager: the splash screen waits on the first settings read (a few ms). */
    val uiState: StateFlow<MainUiState> = settingsRepository.settings
        .map<UserSettings, MainUiState> { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}
