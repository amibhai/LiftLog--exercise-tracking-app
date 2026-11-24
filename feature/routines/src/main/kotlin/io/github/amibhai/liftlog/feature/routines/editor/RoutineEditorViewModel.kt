package io.github.amibhai.liftlog.feature.routines.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.workout.editExercise
import io.github.amibhai.liftlog.core.domain.workout.insertExercise
import io.github.amibhai.liftlog.core.domain.workout.leaveSuperset
import io.github.amibhai.liftlog.core.domain.workout.removeExercise
import io.github.amibhai.liftlog.core.domain.workout.reorderExercises
import io.github.amibhai.liftlog.core.domain.workout.supersetLetters
import io.github.amibhai.liftlog.core.domain.workout.supersetWith
import io.github.amibhai.liftlog.feature.routines.navigation.RoutineEditorDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject
import kotlin.math.roundToInt

/** Plans a routine (SPEC §14: routines CRUD). Every change is stored at once; there is no save button. */
@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routines: RoutineRepository,
    private val work: SequentialWork,
    private val clock: Clock,
    settings: SettingsRepository,
) : ViewModel() {

    private val id = RoutineId(savedStateHandle.toRoute<RoutineEditorDestination>().routineId)
    private val message = MutableStateFlow<RoutineEditorMessage?>(null)
    private var undo: ((Routine) -> Routine)? = null

    private class Stored(val routine: Routine?)

    private val stored: StateFlow<Stored?> = routines.observe(id).map(::Stored)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val userSettings: StateFlow<UserSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val uiState: StateFlow<RoutineEditorUiState> = combine(stored, userSettings, message) { stored, settings, message ->
        when {
            stored == null -> RoutineEditorUiState.Loading
            stored.routine == null -> RoutineEditorUiState.Gone
            else -> ready(stored.routine, settings, message)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoutineEditorUiState.Loading)

    fun onAction(action: RoutineEditorAction) {
        when (action) {
            is RoutineEditorAction.OnExercise -> onExercise(action)

            is RoutineEditorAction.Rename -> edit { it.copy(name = action.name.trim()) }

            is RoutineEditorAction.Reorder -> edit { it.reorderExercises(action.ids) }

            RoutineEditorAction.Undo -> {
                undo?.let(::edit)
                undo = null
                message.value = null
            }

            RoutineEditorAction.DismissMessage -> message.value = null
        }
    }

    private fun onExercise(action: RoutineEditorAction.OnExercise) {
        when (action) {
            is RoutineEditorAction.Remove -> remove(action)

            is RoutineEditorAction.Superset -> edit { it.supersetWith(action.id, action.others) }

            is RoutineEditorAction.LeaveSuperset -> edit { it.leaveSuperset(action.id) }

            is RoutineEditorAction.SetProgression -> {
                val now = clock.millis()
                edit { routine -> routine.editExercise(action.id) { it.progressingBy(action.rule, now) } }
            }

            else -> {
                val mode = userSettings.value.effortMode
                edit { routine -> routine.editExercise(action.id) { it.changedBy(action, mode) } }
            }
        }
    }

    private fun remove(action: RoutineEditorAction.Remove) {
        val original = stored.value?.routine ?: return
        val index = original.exercises.indexOfFirst { it.id == action.id }
        val removed = original.exercises.getOrNull(index) ?: return
        undo = { it.insertExercise(index, removed, original) }
        edit { it.removeExercise(action.id) }
        message.value = RoutineEditorMessage.EXERCISE_REMOVED
    }

    private fun edit(change: (Routine) -> Routine) = work.submit { routines.edit(id, change) }

    /** A new routine that was left without a name or exercises is not worth keeping. */
    override fun onCleared() {
        // In the queue, so that it sees every change made before.
        work.submit {
            val routine = routines.get(id)
            if (routine != null && routine.name.isBlank() && routine.exercises.isEmpty()) routines.delete(id)
        }
    }

    private fun ready(routine: Routine, settings: UserSettings, message: RoutineEditorMessage?) =
        RoutineEditorUiState.Ready(
            id = routine.id,
            name = routine.name,
            exercises = routine.exercises.zip(
                supersetLetters(routine.exercises) { it.supersetGroup },
            ) { exercise, letter ->
                RoutineExerciseUiModel(
                    id = exercise.id,
                    exerciseId = exercise.exercise.id,
                    name = exercise.exercise.name,
                    supersetLetter = letter,
                    targetSets = exercise.targetSets,
                    repMin = exercise.repMin,
                    repMax = exercise.repMax,
                    restSeconds = exercise.restSeconds,
                    defaultRestSeconds = exercise.exercise.defaults.restSeconds ?: settings.defaultRestSeconds,
                    effortTarget = when (settings.effortMode) {
                        EffortMode.RPE -> exercise.targetRpe
                        EffortMode.RIR -> exercise.targetRir?.toDouble()
                        EffortMode.OFF -> null
                    },
                    notes = exercise.notes,
                    progression = exercise.progression,
                )
            },
            effortMode = settings.effortMode,
            unit = settings.weightUnit,
            message = message,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private const val MAX_SETS = 20

/** A new training max starts its first week now. */
private fun RoutineExercise.progressingBy(rule: ProgressionRule, now: Long): RoutineExercise {
    val old = (progression as? ProgressionRule.FiveThreeOne)?.trainingMaxKg
    val isNewMax = rule is ProgressionRule.FiveThreeOne && rule.trainingMaxKg != old
    return copy(progression = if (isNewMax) rule.copy(since = now) else rule)
}

private fun RoutineExercise.changedBy(action: RoutineEditorAction.OnExercise, mode: EffortMode) = when (action) {
    is RoutineEditorAction.SetSets -> copy(targetSets = action.sets.coerceIn(1, MAX_SETS))

    is RoutineEditorAction.SetReps -> copy(repMin = action.min, repMax = action.max)

    is RoutineEditorAction.SetRest -> copy(restSeconds = action.seconds)

    is RoutineEditorAction.SetEffort -> when (mode) {
        EffortMode.RIR -> copy(targetRir = action.value?.roundToInt())
        else -> copy(targetRpe = action.value)
    }

    is RoutineEditorAction.SetNotes -> copy(notes = action.notes.trim().ifEmpty { null })

    else -> this
}
