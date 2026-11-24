package io.github.amibhai.liftlog.feature.routines.editor

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.units.WeightUnit

sealed interface RoutineEditorUiState {
    data object Loading : RoutineEditorUiState

    /** Deleted elsewhere: the screen closes. */
    data object Gone : RoutineEditorUiState

    @Immutable
    data class Ready(
        val id: RoutineId,
        /** Blank until it is named. */
        val name: String,
        val exercises: List<RoutineExerciseUiModel>,
        val effortMode: EffortMode,
        val unit: WeightUnit,
        val message: RoutineEditorMessage? = null,
    ) : RoutineEditorUiState
}

/** One planned exercise: how many sets, how many reps, how long a rest, how hard. */
@Immutable
data class RoutineExerciseUiModel(
    val id: RoutineExerciseId,
    val exerciseId: ExerciseId,
    val name: String,
    val supersetLetter: Char?,
    val targetSets: Int,
    val repMin: Int?,
    val repMax: Int?,
    /** Null means [defaultRestSeconds]. */
    val restSeconds: Int?,
    val defaultRestSeconds: Int,
    /** RPE or RIR, as the settings choose. */
    val effortTarget: Double?,
    val notes: String?,
    val progression: ProgressionRule,
)

/** Something that can be undone. */
enum class RoutineEditorMessage { EXERCISE_REMOVED, }

/** Where the routine editor leads. */
@Stable
class RoutineEditorNavigation(
    val onBack: () -> Unit,
    val onAddExercises: (RoutineId) -> Unit,
    val onReplaceExercise: (RoutineId, RoutineExerciseId) -> Unit,
    val onOpenExercise: (ExerciseId) -> Unit,
)
