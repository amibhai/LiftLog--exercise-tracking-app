package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.plates.warmupSets
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.workout.addSet
import io.github.amibhai.liftlog.core.domain.workout.adjustedBy
import io.github.amibhai.liftlog.core.domain.workout.editExercise
import io.github.amibhai.liftlog.core.domain.workout.finish
import io.github.amibhai.liftlog.core.domain.workout.insertExercise
import io.github.amibhai.liftlog.core.domain.workout.leaveSuperset
import io.github.amibhai.liftlog.core.domain.workout.removeExercise
import io.github.amibhai.liftlog.core.domain.workout.reorderExercises
import io.github.amibhai.liftlog.core.domain.workout.supersetWith
import io.github.amibhai.liftlog.core.domain.workout.withWarmups
import io.github.amibhai.liftlog.core.domain.workout.workingWeight

/** What can be done with whole exercises and the workout itself. */
internal class ExerciseHandler(private val session: WorkoutSession, private val exercises: ExerciseEditor) {

    fun handle(action: WorkoutAction.OnExercise) {
        when (action) {
            is WorkoutAction.AddSet -> {
                val id = WorkoutSetId(session.editor.newId())
                session.edit { it.addSet(action.id, id) }
            }

            is WorkoutAction.AddWarmups -> addWarmups(action.id)

            is WorkoutAction.RemoveExercise -> remove(action.id)

            is WorkoutAction.SetNotes -> session.edit { workout ->
                workout.editExercise(action.id) { it.copy(notes = action.notes.trimmedOrNull()) }
            }

            is WorkoutAction.SetStickyNote -> session.editor.submit {
                exercises.setStickyNote(action.id, action.note.trimmedOrNull())
            }

            is WorkoutAction.SetRest -> session.edit { workout ->
                workout.editExercise(action.id) { it.copy(restSeconds = action.seconds) }
            }

            is WorkoutAction.Superset -> session.edit { it.supersetWith(action.id, action.others) }

            is WorkoutAction.LeaveSuperset -> session.edit { it.leaveSuperset(action.id) }

            is WorkoutAction.Reorder -> session.edit { it.reorderExercises(action.ids) }
        }
    }

    fun handle(action: WorkoutAction.OnWorkout) {
        when (action) {
            is WorkoutAction.Rename -> session.edit { it.copy(name = action.name.trimmedOrNull()) }

            is WorkoutAction.SetWorkoutNotes -> session.edit { it.copy(notes = action.notes.trimmedOrNull()) }

            is WorkoutAction.SetTimes ->
                session.edit {
                    it.copy(
                        startedAt = action.startedAt,
                        endedAt = maxOf(action.startedAt, action.endedAt),
                    )
                }

            is WorkoutAction.AdjustRest -> {
                val now = session.editor.now()
                session.edit { it.copy(restTimer = it.restTimer?.adjustedBy(action.seconds, now)) }
            }

            WorkoutAction.SkipRest -> session.edit { it.copy(restTimer = null) }

            WorkoutAction.Finish -> {
                val now = session.editor.now()
                session.edit { it.finish(now) }
                session.updateLocal { it.copy(editing = null, isFinished = true) }
            }

            WorkoutAction.Discard -> session.editor.delete(session.id)
        }
    }

    private fun addWarmups(id: WorkoutExerciseId) {
        val exercise = session.current?.exercises?.firstOrNull { it.id == id }
        val weight = exercise?.workingWeight ?: return
        val warmups = warmupSets(exercise.exercise, weight, session.settings).map {
            WorkoutSet(WorkoutSetId(session.editor.newId()), SetType.WARMUP, target = it)
        }
        session.undo = { workout -> workout.editExercise(id) { it.copy(sets = exercise.sets) } }
        session.edit { it.withWarmups(id, warmups) }
        session.updateLocal { it.copy(editing = null, message = WorkoutMessage.WARMUPS_ADDED) }
    }

    private fun remove(id: WorkoutExerciseId) {
        val original = session.current ?: return
        val index = original.exercises.indexOfFirst { it.id == id }
        val removed = original.exercises.getOrNull(index) ?: return
        session.undo = { it.insertExercise(index, removed, original) }
        session.edit { it.removeExercise(id) }
        session.updateLocal { it.copy(editing = null, message = WorkoutMessage.EXERCISE_REMOVED) }
    }

    private fun String.trimmedOrNull() = trim().ifEmpty { null }
}
