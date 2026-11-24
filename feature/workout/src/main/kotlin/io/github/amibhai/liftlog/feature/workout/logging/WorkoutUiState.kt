package io.github.amibhai.liftlog.feature.workout.logging

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.progression.SuggestionReason
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.PlateBarUiModel
import io.github.amibhai.liftlog.core.ui.component.RestTimerUiState
import io.github.amibhai.liftlog.core.ui.component.SetRowUiModel
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState

    /** Discarded or deleted: the screen closes. */
    data object Gone : WorkoutUiState

    @Immutable
    data class Ready(
        val id: WorkoutId,
        val title: WorkoutTitle,
        val startedAt: Long,
        val endedAt: Long?,
        /** False while an earlier workout is being edited. */
        val isInProgress: Boolean,
        val notes: String?,
        /** With its unit: "3,240 kg". */
        val volume: String,
        val completedSets: Int,
        val totalSets: Int,
        val items: List<WorkoutItem>,
        val exercises: List<ExerciseRef>,
        val restTimer: RestTimerUiState? = null,
        val pad: PadUiModel? = null,
        /** The set to bring into view. */
        val focus: FocusTarget? = null,
        val message: WorkoutMessage? = null,
        val keepScreenOn: Boolean = false,
        /** Set once the workout has been finished: its summary opens. */
        val isFinished: Boolean = false,
    ) : WorkoutUiState
}

/** An exercise in the workout, for the dialogs that list them. */
@Immutable
data class ExerciseRef(val id: WorkoutExerciseId, val name: String, val supersetLetter: Char?)

/** One element of the workout list. */
@Immutable
sealed interface WorkoutItem {
    val key: String

    data class Header(
        val id: WorkoutExerciseId,
        val exerciseId: ExerciseId,
        val name: String,
        val supersetLetter: Char?,
        val stickyNote: String?,
        val notes: String?,
        /** Set for this workout; null means [defaultRestSeconds]. */
        val restSeconds: Int?,
        /** The exercise's own rest time, or the one in the settings. */
        val defaultRestSeconds: Int,
        val columns: List<ColumnUiModel>,
        /** There is a working weight to warm up for. */
        val canWarmUp: Boolean = false,
        /** Why the sets are planned as they are. */
        val reason: SuggestionReason? = null,
        val unit: WeightUnit = WeightUnit.KG,
    ) : WorkoutItem {
        override val key: String get() = "header-${id.value}"
    }

    data class Set(val row: SetRowUiModel) : WorkoutItem {
        override val key: String get() = row.id
    }

    /** The rating of the set above, right after it was completed. */
    data class Effort(val setId: WorkoutSetId, val mode: EffortMode, val selected: Double?) : WorkoutItem {
        override val key: String get() = "effort-${setId.value}"
    }

    data class AddSet(val id: WorkoutExerciseId) : WorkoutItem {
        override val key: String get() = "add-${id.value}"
    }
}

/** A column title: the unit for weights and distances, a word for the rest. */
@Immutable
data class ColumnUiModel(val field: SetField, val unit: String?)

/** The number pad, for the field being edited. */
@Immutable
data class PadUiModel(
    val field: SetField,
    val unit: String?,
    val display: String,
    val step: String?,
    val allowsDecimal: Boolean,
    val canUsePrevious: Boolean,
    val isLastField: Boolean,
    /** How to load the weight; null without a bar. */
    val plates: PlateBarUiModel? = null,
)

/** [serial] makes the same set a new target when it is focused again. */
@Immutable
data class FocusTarget(val setId: String, val serial: Int)

/** Something that can be undone. */
enum class WorkoutMessage { SET_DELETED, EXERCISE_REMOVED, WARMUPS_ADDED }
