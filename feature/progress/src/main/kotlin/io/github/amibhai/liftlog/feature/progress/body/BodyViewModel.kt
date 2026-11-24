package io.github.amibhai.liftlog.feature.progress.body

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.ProgressPhotoId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.ProgressPhoto
import io.github.amibhai.liftlog.core.domain.repository.BodyRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@Immutable
data class BodyUiState(
    val unit: WeightUnit = WeightUnit.KG,
    /** Every kind of measurement, with the latest value of those that have one. */
    val metrics: List<MetricRow> = emptyList(),
    val photos: List<ProgressPhoto> = emptyList(),
    /** A photo that can still be brought back. */
    val deletedPhoto: ProgressPhotoId? = null,
    val hasFailedToAdd: Boolean = false,
)

@Immutable
data class MetricRow(val type: BodyMetricType, val latest: BodyMetric?)

private data class BodyLocalState(val deletedPhoto: ProgressPhotoId? = null, val hasFailedToAdd: Boolean = false)

/** Body weight, measurements and progress photos (SPEC §8). */
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val body: BodyRepository,
    settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val local = MutableStateFlow(BodyLocalState())

    val uiState: StateFlow<BodyUiState> = combine(
        body.observeLatestMetrics(),
        body.observePhotos(),
        settings.settings,
        local,
    ) { latest, photos, current, local ->
        val byType = latest.associateBy { it.type }
        BodyUiState(
            unit = current.weightUnit,
            metrics = BodyMetricType.entries.map { MetricRow(it, byType[it]) },
            photos = photos,
            deletedPhoto = local.deletedPhoto,
            hasFailedToAdd = local.hasFailedToAdd,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BodyUiState())

    fun addPhoto(sourceUri: String) {
        viewModelScope.launch {
            val added = body.addPhoto(sourceUri, clock.millis())
            local.update { it.copy(hasFailedToAdd = added == null) }
        }
    }

    fun deletePhoto(id: ProgressPhotoId) {
        viewModelScope.launch { body.deletePhoto(id) }
        local.update { it.copy(deletedPhoto = id) }
    }

    fun undoDeletePhoto() {
        val id = local.value.deletedPhoto ?: return
        viewModelScope.launch { body.restorePhoto(id) }
        onMessageShown()
    }

    /** Called when a message has been read, or the undo chance has gone. */
    fun onMessageShown() = local.update { BodyLocalState() }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
