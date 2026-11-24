package io.github.amibhai.liftlog.feature.progress.body

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.TrendPoint
import io.github.amibhai.liftlog.core.domain.progress.canonicalValue
import io.github.amibhai.liftlog.core.domain.progress.movingAverage
import io.github.amibhai.liftlog.core.domain.repository.BodyRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.feature.progress.navigation.MetricDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@Immutable
data class MetricUiState(
    val type: BodyMetricType,
    val unit: WeightUnit,
    /** Newest first. */
    val entries: List<BodyMetric> = emptyList(),
    /** The trend, oldest first; only with enough entries to have one. */
    val trend: List<TrendPoint> = emptyList(),
    /** An entry that can still be brought back. */
    val deleted: BodyMetricId? = null,
)

/** One kind of measurement: its history, with a trend (SPEC §8). */
@HiltViewModel
class MetricViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val body: BodyRepository,
    settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) : ViewModel() {

    private val type = BodyMetricType.valueOf(savedStateHandle.toRoute<MetricDestination>().type)
    private val deleted = MutableStateFlow<BodyMetricId?>(null)

    val uiState: StateFlow<MetricUiState> = combine(
        body.observeMetrics(type),
        settings.settings,
        deleted,
    ) { metrics, current, deleted ->
        MetricUiState(
            type = type,
            unit = current.weightUnit,
            entries = metrics.sortedByDescending { it.measuredAt },
            trend = if (metrics.size < MIN_TREND) emptyList() else movingAverage(metrics),
            deleted = deleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MetricUiState(type, WeightUnit.KG))

    /** [shown] is what was typed, in the unit in use. */
    fun add(shown: Double, at: Long) = save(BodyMetric(BodyMetricId(ids.newId()), type, canonical(shown), at))

    fun edit(metric: BodyMetric, shown: Double, at: Long) = save(metric.copy(value = canonical(shown), measuredAt = at))

    fun delete(id: BodyMetricId) {
        viewModelScope.launch { body.deleteMetric(id) }
        deleted.value = id
    }

    fun undoDelete() {
        val id = deleted.value ?: return
        viewModelScope.launch { body.restoreMetric(id) }
        deleted.value = null
    }

    fun onMessageShown() {
        deleted.value = null
    }

    private fun canonical(shown: Double): Double = canonicalValue(type, shown, uiState.value.unit)

    private fun save(metric: BodyMetric) {
        viewModelScope.launch { body.saveMetric(metric) }
    }

    private companion object {
        const val MIN_TREND = 3
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
