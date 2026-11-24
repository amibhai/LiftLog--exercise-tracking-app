package io.github.amibhai.liftlog.feature.workout.logging

/** The dialog that is open, if any. */
internal sealed interface WorkoutDialog {
    data object Rename : WorkoutDialog

    data object Notes : WorkoutDialog

    data object Reorder : WorkoutDialog

    data object Finish : WorkoutDialog

    /** There is nothing to finish yet. */
    data object NothingLogged : WorkoutDialog

    data object Discard : WorkoutDialog

    data object StartTime : WorkoutDialog

    data object EndTime : WorkoutDialog

    data class ExerciseNotes(val header: WorkoutItem.Header) : WorkoutDialog

    data class StickyNote(val header: WorkoutItem.Header) : WorkoutDialog

    data class Rest(val header: WorkoutItem.Header) : WorkoutDialog

    data class Superset(val header: WorkoutItem.Header) : WorkoutDialog
}

/** Finishing always asks, because it removes what is not done. */
internal fun finishDialog(state: WorkoutUiState.Ready): WorkoutDialog =
    if (state.completedSets == 0) WorkoutDialog.NothingLogged else WorkoutDialog.Finish
