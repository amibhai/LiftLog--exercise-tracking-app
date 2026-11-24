package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** The workout on screen as the action handlers see it. */
internal class WorkoutSession(
    val id: WorkoutId,
    val editor: WorkoutEditor,
    private val workout: StateFlow<Workout?>,
    private val previousSets: StateFlow<Map<ExerciseId, List<LoggedSet>>>,
    private val userSettings: StateFlow<UserSettings>,
) {
    val local = MutableStateFlow(WorkoutLocalState())

    /** Takes back the last deletion. */
    var undo: ((Workout) -> Workout)? = null

    val current: Workout? get() = workout.value

    val previous: Map<ExerciseId, List<LoggedSet>> get() = previousSets.value

    val settings: UserSettings get() = userSettings.value

    fun edit(change: (Workout) -> Workout) = editor.edit(id, change)

    fun updateLocal(change: (WorkoutLocalState) -> WorkoutLocalState) = local.update(change)

    fun exerciseOf(setId: WorkoutSetId): WorkoutExercise? =
        current?.exercises?.firstOrNull { exercise -> exercise.sets.any { it.id == setId } }
}
