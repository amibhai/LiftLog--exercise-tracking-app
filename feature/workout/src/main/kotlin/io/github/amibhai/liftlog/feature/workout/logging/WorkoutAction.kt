package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.ui.component.NumberPadKey
import io.github.amibhai.liftlog.core.ui.component.SetRowAction

/** Everything the workout screen can ask for. */
sealed interface WorkoutAction {

    sealed interface OnSet : WorkoutAction

    data class Row(val action: SetRowAction) : OnSet

    data class Rate(val setId: WorkoutSetId, val value: Double?) : OnSet

    data class PadKey(val key: NumberPadKey) : WorkoutAction

    sealed interface OnExercise : WorkoutAction

    data class AddSet(val id: WorkoutExerciseId) : OnExercise

    /** Warm-up sets for the working weight, from the steps in the settings. */
    data class AddWarmups(val id: WorkoutExerciseId) : OnExercise

    data class RemoveExercise(val id: WorkoutExerciseId) : OnExercise

    data class SetNotes(val id: WorkoutExerciseId, val notes: String) : OnExercise

    data class SetStickyNote(val id: ExerciseId, val note: String) : OnExercise

    /** Null for the default. */
    data class SetRest(val id: WorkoutExerciseId, val seconds: Int?) : OnExercise

    data class Superset(val id: WorkoutExerciseId, val others: Set<WorkoutExerciseId>) : OnExercise

    data class LeaveSuperset(val id: WorkoutExerciseId) : OnExercise

    data class Reorder(val ids: List<WorkoutExerciseId>) : OnExercise

    sealed interface OnWorkout : WorkoutAction

    data class Rename(val name: String) : OnWorkout

    data class SetWorkoutNotes(val notes: String) : OnWorkout

    data class SetTimes(val startedAt: Long, val endedAt: Long) : OnWorkout

    data class AdjustRest(val seconds: Int) : OnWorkout

    data object SkipRest : OnWorkout

    data object Finish : OnWorkout

    data object Discard : OnWorkout

    data object Undo : WorkoutAction

    data object DismissMessage : WorkoutAction
}
