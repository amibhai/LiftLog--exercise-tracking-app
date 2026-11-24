package io.github.amibhai.liftlog.feature.history

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import java.time.YearMonth

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    @Immutable
    data class Ready(val items: List<HistoryItem>, val canLoadMore: Boolean) : HistoryUiState
}

/** The history list: month headers, each followed by that month's workouts, newest first. */
@Immutable
sealed interface HistoryItem {
    val key: String

    /** [at] is the start of the first workout listed under it. */
    data class Month(val month: YearMonth, val at: Long) : HistoryItem {
        override val key: String get() = "month-$month"
    }

    data class Entry(
        val id: WorkoutId,
        val title: WorkoutTitle,
        val startedAt: Long,
        val durationSeconds: Int,
        /** With its unit: "3,240 kg". */
        val volume: String,
        val sets: Int,
        val exercises: List<ExerciseLine>,
        /** Exercises not listed in [exercises]. */
        val moreExercises: Int,
    ) : HistoryItem {
        override val key: String get() = id.value
    }
}

/** "3 × Bench Press", with the best set: "80 kg × 8". */
@Immutable
data class ExerciseLine(val name: String, val sets: Int, val best: String?)
