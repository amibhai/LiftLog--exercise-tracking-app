package io.github.amibhai.liftlog.feature.workout.logging

import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.model.missingField
import io.github.amibhai.liftlog.core.domain.units.enter
import io.github.amibhai.liftlog.core.domain.workout.completeSet
import io.github.amibhai.liftlog.core.domain.workout.duplicateSet
import io.github.amibhai.liftlog.core.domain.workout.findSet
import io.github.amibhai.liftlog.core.domain.workout.insertSet
import io.github.amibhai.liftlog.core.domain.workout.logSet
import io.github.amibhai.liftlog.core.domain.workout.nextStepAfter
import io.github.amibhai.liftlog.core.domain.workout.rateSet
import io.github.amibhai.liftlog.core.domain.workout.removeSet
import io.github.amibhai.liftlog.core.domain.workout.reopenSet
import io.github.amibhai.liftlog.core.domain.workout.updateSet
import io.github.amibhai.liftlog.core.ui.component.NumberPadKey
import io.github.amibhai.liftlog.core.ui.component.SetRowAction

/** What can be done with single sets, and the number pad that edits them. */
internal class SetHandler(private val session: WorkoutSession) {

    fun handle(action: WorkoutAction.OnSet) {
        when (action) {
            is WorkoutAction.Row -> handle(action.action)
            is WorkoutAction.Rate -> rate(action.setId, action.value)
        }
    }

    fun press(key: NumberPadKey) {
        val editing = session.local.value.editing ?: return
        val exercise = session.exerciseOf(editing.setId)
        val next = exercise?.let { nextField(it, editing) }
        when {
            exercise == null || key == NumberPadKey.Hide -> stopEditing()
            key == NumberPadKey.Next -> if (next == null) stopEditing() else startEditing(next.first, next.second)
            else -> type(editing, exercise, key)
        }
    }

    private fun stopEditing() = session.updateLocal { it.copy(editing = null) }

    private fun handle(action: SetRowAction) {
        val id = WorkoutSetId(action.setId)
        val exercise = session.exerciseOf(id) ?: return
        val set = exercise.sets.first { it.id == id }
        when (action) {
            is SetRowAction.EditField -> startEditing(set, action.field)

            is SetRowAction.ToggleComplete -> {
                // What the row shows, which may be ahead of the stored workout.
                val isDone = session.local.value.pendingDone[id] ?: set.isCompleted
                if (isDone) reopen(id) else complete(set, exercise)
            }

            is SetRowAction.ChangeType -> session.edit { it.updateSet(id) { set -> set.copy(type = action.type) } }

            is SetRowAction.UsePrevious -> usePrevious(id, exercise)

            is SetRowAction.Duplicate -> {
                val copy = WorkoutSetId(session.editor.newId())
                session.edit { it.duplicateSet(id, copy) }
            }

            is SetRowAction.Delete -> delete(id)
        }
    }

    private fun startEditing(set: WorkoutSet, field: SetField) {
        val exercise = session.exerciseOf(set.id) ?: return
        val editing = PadEditing.start(set, field, exercise.exercise.type, session.settings.weightUnit)
        session.updateLocal { it.copy(editing = editing, focus = it.focusOn(set.id)) }
    }

    private fun type(editing: PadEditing, exercise: WorkoutExercise, key: NumberPadKey) {
        val settings = session.settings
        val input = editing.input.reduce(
            key = key,
            step = editing.step(exercise.exercise, settings),
            previous = previousValue(exercise, editing, session.previous, settings.weightUnit),
        )
        session.updateLocal { it.copy(editing = editing.copy(input = input)) }
        session.edit { workout ->
            workout.updateSet(editing.setId) { set ->
                val type = exercise.exercise.type
                set.copy(entered = set.entered.enter(editing.field, input.value, type, settings.weightUnit))
            }
        }
    }

    /** One tap logs the set as shown; without the numbers it needs, the pad asks for them. */
    private fun complete(set: WorkoutSet, exercise: WorkoutExercise) {
        val missing = exercise.exercise.type.missingField(set.shown)
        val workout = session.current
        if (missing != null || workout == null) {
            startEditing(set, missing ?: exercise.exercise.type.fields.first())
            return
        }
        val now = session.editor.now()
        val settings = session.settings
        session.edit { stored ->
            if (stored.isInProgress) {
                stored.logSet(set.id, now, settings)
            } else {
                stored.completeSet(set.id, stored.endedAt ?: now)
            }
        }
        val next = workout.completeSet(set.id, now).nextStepAfter(set.id).setId
        val rates = settings.effortMode != EffortMode.OFF && set.type != SetType.WARMUP
        session.updateLocal {
            it.copy(
                editing = null,
                effortFor = set.id.takeIf { rates },
                pendingDone = it.pendingDone + (set.id to true),
                focus = if (workout.isInProgress && next != null) it.focusOn(next) else it.focus,
            )
        }
    }

    private fun reopen(id: WorkoutSetId) {
        session.edit { it.reopenSet(id) }
        session.updateLocal {
            it.copy(
                effortFor = it.effortFor.takeIf { setId -> setId != id },
                pendingDone = it.pendingDone + (id to false),
            )
        }
    }

    private fun rate(id: WorkoutSetId, value: Double?) {
        val mode = session.settings.effortMode
        session.edit { it.rateSet(id, mode, value) }
        // One tap is enough: the chips go once the set is rated.
        if (value != null) session.updateLocal { it.copy(effortFor = null) }
    }

    private fun usePrevious(id: WorkoutSetId, exercise: WorkoutExercise) {
        val values = previousSet(exercise, id, session.previous)?.values ?: return
        session.updateLocal { it.copy(editing = null) }
        session.edit { it.updateSet(id) { set -> set.copy(entered = values.forType(exercise.exercise.type)) } }
    }

    private fun delete(id: WorkoutSetId) {
        val removed = session.current?.findSet(id) ?: return
        session.undo = { it.insertSet(removed.exerciseId, removed.index, removed.set) }
        session.edit { it.removeSet(id) }
        session.updateLocal { it.copy(editing = null, message = WorkoutMessage.SET_DELETED) }
    }
}

internal fun WorkoutLocalState.focusOn(setId: WorkoutSetId) =
    FocusTarget(setId.value, serial = (focus?.serial ?: 0) + 1)
