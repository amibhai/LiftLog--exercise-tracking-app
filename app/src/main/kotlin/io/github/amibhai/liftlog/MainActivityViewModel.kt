package io.github.amibhai.liftlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import io.github.amibhai.liftlog.core.ui.launch.LaunchAction
import io.github.amibhai.liftlog.ui.AppTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val settings: UserSettings, val showOnboarding: Boolean = false) : MainUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val workouts: WorkoutRepository,
    private val startWorkout: StartWorkout,
) : ViewModel() {
    /** Eager: the splash screen waits on the first settings read (a few ms). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MainUiState> = settings.settings
        .flatMapLatest { if (it.onboardingDone) flowOf(MainUiState.Ready(it)) else beforeOnboarding(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    private val asked = MutableStateFlow<AppTarget?>(null)

    /** The screen to open next. */
    val target: StateFlow<AppTarget?> = asked.asStateFlow()

    fun onLaunch(action: LaunchAction) {
        viewModelScope.launch {
            val id = when (action) {
                LaunchAction.START_EMPTY -> startWorkout.empty()
                LaunchAction.START_NEXT -> startWorkout.next()
                LaunchAction.REPEAT_LAST -> startWorkout.repeatLast() ?: startWorkout.empty()
            }
            asked.value = AppTarget.ActiveWorkout(id)
        }
    }

    fun onOnboardingFinished(openPrograms: Boolean) {
        if (openPrograms) asked.value = AppTarget.Programs
    }

    fun onTargetOpened() {
        asked.value = null
    }

    /** The first-run pages are for those who have not trained with the app yet. */
    private fun beforeOnboarding(current: UserSettings): Flow<MainUiState> =
        combine(workouts.observeActive(), workouts.observeFinishedCount()) { active, n -> active != null || n > 0 }
            .distinctUntilChanged()
            .onEach { hasTrained -> if (hasTrained) settings.update { it.copy(onboardingDone = true) } }
            .map { hasTrained -> MainUiState.Ready(current, showOnboarding = !hasTrained) }
}
