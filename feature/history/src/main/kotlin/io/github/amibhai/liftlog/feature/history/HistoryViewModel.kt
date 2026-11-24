package io.github.amibhai.liftlog.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

/** Finished workouts, newest first, a page at a time (SPEC §14: history list). */
@HiltViewModel
class HistoryViewModel @Inject constructor(workouts: WorkoutRepository, settings: SettingsRepository) : ViewModel() {

    private val limit = MutableStateFlow(PAGE_SIZE)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(
        limit.flatMapLatest { workouts.observeFinished(it) },
        workouts.observeFinishedCount(),
        settings.settings,
    ) { finished, count, settings ->
        if (finished.isEmpty()) {
            HistoryUiState.Empty
        } else {
            HistoryUiState.Ready(historyItems(finished, settings, ZoneId.systemDefault()), finished.size < count)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

    /** Asks for the next page, once the current one is shown. */
    fun loadMore() {
        val state = uiState.value as? HistoryUiState.Ready ?: return
        val shown = state.items.count { it is HistoryItem.Entry }
        if (state.canLoadMore && shown >= limit.value) limit.value += PAGE_SIZE
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
