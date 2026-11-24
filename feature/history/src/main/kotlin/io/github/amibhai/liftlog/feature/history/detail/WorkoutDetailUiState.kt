package io.github.amibhai.liftlog.feature.history.detail

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle

sealed interface WorkoutDetailUiState {
    data object Loading : WorkoutDetailUiState

    /** Deleted: the screen closes. */
    data object Gone : WorkoutDetailUiState

    @Immutable
    data class Ready(
        val id: WorkoutId,
        val title: WorkoutTitle,
        val startedAt: Long,
        val durationSeconds: Int,
        val volume: String,
        val sets: Int,
        val notes: String?,
        val exercises: List<DetailExercise>,
        val routine: RoutineOffer,
        /** Opened right after finishing the workout. */
        val justFinished: Boolean,
        /** The workout in progress, when it is another one: starting a new one has to wait for it. */
        val activeWorkoutId: WorkoutId?,
        val routineDone: RoutineDone? = null,
        /** A workout that was just started from this one, for the screen to open. */
        val openWorkout: WorkoutId? = null,
    ) : WorkoutDetailUiState
}

@Immutable
data class DetailExercise(
    val id: WorkoutExerciseId,
    val exerciseId: ExerciseId,
    val name: String,
    val supersetLetter: Char?,
    val notes: String?,
    val sets: List<DetailSet>,
)

/** [number] counts working sets; [text] is "80 kg × 8"; [effort] is "RPE 8.5". */
@Immutable
data class DetailSet(val type: SetType, val number: Int?, val text: String, val effort: String?)

/** What this workout can do for routines (SPEC §5: save as or update a routine). */
@Immutable
sealed interface RoutineOffer {
    data object Save : RoutineOffer

    data class Update(val name: String) : RoutineOffer

    /** The workout followed its routine as it is. */
    data object None : RoutineOffer
}

@Immutable
sealed interface RoutineDone {
    val name: String

    data class Saved(override val name: String) : RoutineDone

    data class Updated(override val name: String) : RoutineDone
}

/** What the detail screen adds to the stored workout. */
internal data class DetailLocalState(val routineDone: RoutineDone? = null, val openWorkout: WorkoutId? = null)
