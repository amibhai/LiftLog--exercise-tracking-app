package io.github.amibhai.liftlog.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.progress.weekStart
import io.github.amibhai.liftlog.core.domain.repository.BodyRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** The training dashboard (SPEC §8). */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progress: ProgressRepository,
    private val body: BodyRepository,
    settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProgressUiState> = settings.settings.flatMapLatest { current ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(clock.withZone(zone))
        val firstDay = current.weekStartsOn()
        val weekStart = weekStart(today, firstDay).atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEnd = weekStart(today, firstDay).plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        combine(
            progress.observeWorkoutStats(),
            progress.observeMuscleWork(weekStart, weekEnd),
            progress.observeRecentRecords(RECORDS),
            body.observeLatestMetrics(),
        ) { stats, work, records, metrics ->
            dashboard(DashboardData(stats, work, records, metrics), current, today, firstDay, zone)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProgressUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val RECORDS = 8
    }
}
