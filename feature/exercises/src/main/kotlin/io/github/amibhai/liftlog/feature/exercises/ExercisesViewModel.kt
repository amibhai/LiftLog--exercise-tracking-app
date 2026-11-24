package io.github.amibhai.liftlog.feature.exercises

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.ExerciseSummary
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class ExercisesUiState(
    val totalExercises: Int? = null,
    /** The query [results] belong to (lags the text field by the debounce). */
    val searchedQuery: String = "",
    val results: List<ExerciseSummary> = emptyList(),
)

/**
 * Phase 0 library browser: debounced FTS prefix search (SPEC §4). The full picker with suggestions,
 * frecency and fuzzy matching arrives in Phase 1.
 */
@HiltViewModel
class ExercisesViewModel @Inject constructor(private val repository: ExerciseRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val results = query
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { searched -> repository.search(searched).map { searched to it } }

    val uiState: StateFlow<ExercisesUiState> = combine(repository.observeStats(), results) { stats, (searched, found) ->
        ExercisesUiState(totalExercises = stats.totalExercises, searchedQuery = searched, results = found)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExercisesUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 100L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
