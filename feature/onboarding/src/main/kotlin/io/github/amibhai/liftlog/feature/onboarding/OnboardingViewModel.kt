package io.github.amibhai.liftlog.feature.onboarding

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingPage { UNIT, PLATES, PROGRAM }

@Immutable
data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.UNIT,
    /** Null until loaded. */
    val settings: UserSettings? = null,
)

/** The first run (SPEC §13): what was chosen is stored at once, so skipping loses nothing. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SettingsRepository,
) : ViewModel() {

    private val page = savedStateHandle.getStateFlow(PAGE, OnboardingPage.UNIT.name)

    val uiState: StateFlow<OnboardingUiState> = combine(page, repository.settings) { page, settings ->
        OnboardingUiState(OnboardingPage.valueOf(page), settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnboardingUiState())

    fun setUnit(unit: WeightUnit) = update { it.copy(weightUnit = unit) }

    fun setBarWeight(weight: Double) = update { it.withBarWeight(weight) }

    /** Plates that are added are there in any number; the settings can limit them. */
    fun togglePlate(weight: Double) = update { settings ->
        val plates = settings.plates(settings.weightUnit)
        val others = plates.filter { it.weight != weight }
        val changed = if (others.size < plates.size) others else plates + Plate(weight)
        settings.withPlates(changed.sortedByDescending { it.weight })
    }

    fun next() = turn(1)

    fun back() = turn(-1)

    fun finish() = update { it.copy(onboardingDone = true) }

    private fun turn(pages: Int) {
        val index = OnboardingPage.valueOf(page.value).ordinal + pages
        OnboardingPage.entries.getOrNull(index)?.let { savedStateHandle[PAGE] = it.name }
    }

    private fun update(change: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(change) }
    }

    private companion object {
        const val PAGE = "page"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
