package io.github.amibhai.liftlog.feature.routines.editor

import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId

/** Everything the routine editor can ask for. */
sealed interface RoutineEditorAction {

    sealed interface OnExercise : RoutineEditorAction {
        val id: RoutineExerciseId
    }

    data class SetSets(override val id: RoutineExerciseId, val sets: Int) : OnExercise

    /** Both null for any number of reps. */
    data class SetReps(override val id: RoutineExerciseId, val min: Int?, val max: Int?) : OnExercise

    /** Null for the default rest. */
    data class SetRest(override val id: RoutineExerciseId, val seconds: Int?) : OnExercise

    /** RPE or RIR, as the settings choose; null for none. */
    data class SetEffort(override val id: RoutineExerciseId, val value: Double?) : OnExercise

    data class SetNotes(override val id: RoutineExerciseId, val notes: String) : OnExercise

    data class Remove(override val id: RoutineExerciseId) : OnExercise

    data class Superset(override val id: RoutineExerciseId, val others: Set<RoutineExerciseId>) : OnExercise

    data class LeaveSuperset(override val id: RoutineExerciseId) : OnExercise

    data class Rename(val name: String) : RoutineEditorAction

    data class Reorder(val ids: List<RoutineExerciseId>) : RoutineEditorAction

    data object Undo : RoutineEditorAction

    data object DismissMessage : RoutineEditorAction
}
