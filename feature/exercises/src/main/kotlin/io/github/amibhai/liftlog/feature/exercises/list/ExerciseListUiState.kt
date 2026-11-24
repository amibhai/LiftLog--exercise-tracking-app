package io.github.amibhai.liftlog.feature.exercises.list

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.Muscle

/** What the list is narrowed down to; a blank filter shows suggestions (SPEC §4). */
@Immutable
data class ExerciseFilter(val query: String = "", val muscle: Muscle? = null, val equipment: Equipment? = null) {
    val isBlank: Boolean get() = query.isBlank() && muscle == null && equipment == null
}

@Immutable
data class ExerciseListUiState(
    val isLoading: Boolean = true,
    val totalExercises: Int = 0,
    /** The filter these rows belong to; it lags the text field by the debounce. */
    val filter: ExerciseFilter = ExerciseFilter(),
    val sections: List<ExerciseSectionUiModel> = emptyList(),
    /** Whether the query names no existing exercise, so that creating it is worth offering. */
    val canCreate: Boolean = false,
) {
    val hasNoResults: Boolean get() = !isLoading && !filter.isBlank && sections.all { it.rows.isEmpty() }
}

enum class ExerciseSectionKind { FREQUENT, GOES_WITH, RECENT, FAVORITES, STARTERS, RESULTS }

@Immutable
data class ExerciseSectionUiModel(val kind: ExerciseSectionKind, val rows: List<ExerciseRowUiModel>)

@Immutable
data class ExerciseRowUiModel(
    val id: ExerciseId,
    val name: String,
    val equipment: Equipment,
    val muscle: Muscle?,
    val thumbnailUrl: String?,
    val highlights: List<IntRange> = emptyList(),
    /** The best set of the last session ("80 kg × 8"); null when never done. */
    val lastSet: String? = null,
    val lastDaysAgo: Int = 0,
    /** Already in the workout or routine it would be added to. */
    val isAdded: Boolean = false,
)
