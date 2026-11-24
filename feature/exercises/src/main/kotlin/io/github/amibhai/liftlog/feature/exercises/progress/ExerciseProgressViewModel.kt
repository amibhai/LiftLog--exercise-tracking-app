package io.github.amibhai.liftlog.feature.exercises.progress

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.progress.BestMark
import io.github.amibhai.liftlog.core.domain.progress.ProgressMetric
import io.github.amibhai.liftlog.core.domain.progress.ProgressRange
import io.github.amibhai.liftlog.core.domain.progress.RepMax
import io.github.amibhai.liftlog.core.domain.progress.bestMarks
import io.github.amibhai.liftlog.core.domain.progress.progressMetrics
import io.github.amibhai.liftlog.core.domain.progress.repMaxes
import io.github.amibhai.liftlog.core.domain.progress.sessionProgress
import io.github.amibhai.liftlog.core.domain.progress.value
import io.github.amibhai.liftlog.core.domain.progress.within
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.feature.exercises.navigation.ExerciseDetailDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import javax.inject.Inject

/** A point of the chart, in the metric's own unit (kg, reps, seconds or meters). */
@Immutable
data class ProgressPoint(val at: Long, val value: Double)

@Immutable
data class ExerciseProgressUiState(
    val type: ExerciseType,
    val unit: WeightUnit,
    /** Only those with values, most useful first. */
    val metrics: List<ProgressMetric>,
    val metric: ProgressMetric?,
    val range: ProgressRange,
    val points: List<ProgressPoint>,
    val marks: List<BestMark>,
    val repMaxes: List<RepMax>,
    val sessionCount: Int,
)

private data class ChartChoice(val metric: ProgressMetric? = null, val range: ProgressRange = ProgressRange.ALL)

/** Charts, records and the rep-max table of one exercise (SPEC §8). */
@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exercises: ExerciseRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val id = ExerciseId(savedStateHandle.toRoute<ExerciseDetailDestination>().exerciseId)
    private val choice = MutableStateFlow(ChartChoice())

    /** Null until loaded, and for an exercise that is gone. */
    val uiState: StateFlow<ExerciseProgressUiState?> = combine(
        exercises.observeDetail(id),
        progress.observeSessions(id),
        settings.settings,
        choice,
    ) { detail, sessions, current, choice ->
        detail?.let { state(it.type, sessions, current, choice) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun selectMetric(metric: ProgressMetric) = choice.update { it.copy(metric = metric) }

    fun selectRange(range: ProgressRange) = choice.update { it.copy(range = range) }

    private fun state(
        type: ExerciseType,
        sessions: List<ExerciseSession>,
        settings: UserSettings,
        choice: ChartChoice,
    ): ExerciseProgressUiState {
        val series = sessionProgress(type, sessions, settings.e1rmFormula)
        val metrics = type.progressMetrics().filter { metric -> series.any { it.value(metric) != null } }
        val metric = choice.metric?.takeIf { it in metrics } ?: metrics.firstOrNull()
        val inRange = series.within(choice.range, clock.millis()) { it.at }
        return ExerciseProgressUiState(
            type = type,
            unit = settings.weightUnit,
            metrics = metrics,
            metric = metric,
            range = choice.range,
            points = inRange.mapNotNull { session ->
                metric?.let(session::value)?.let { ProgressPoint(session.at, it) }
            },
            marks = bestMarks(type, sessions, settings.e1rmFormula),
            repMaxes = repMaxes(sessions),
            sessionCount = sessions.size,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
