package io.github.amibhai.liftlog.feature.workout.logging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.feature.workout.navigation.WorkoutDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logging a workout, or editing an earlier one (SPEC §5). The stored workout is the only source of truth;
 * the ViewModel adds what is just on screen, such as the number pad.
 */
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val editor: WorkoutEditor,
    pastSessions: PastSessions,
    settings: SettingsRepository,
    exerciseEditor: ExerciseEditor,
) : ViewModel() {

    private val id = WorkoutId(savedStateHandle.toRoute<WorkoutDestination>().workoutId)

    private class Stored(val workout: Workout?)

    private val stored: StateFlow<Stored?> = editor.observe(id).map(::Stored)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val userSettings: StateFlow<UserSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val past: StateFlow<Past> = pastSessions
        .observe(
            workout = stored.map { it?.workout }.filterNotNull(),
            formula = userSettings.map { it.e1rmFormula },
        )
        .stateIn(viewModelScope, SharingStarted.Eagerly, Past())

    private val session = WorkoutSession(
        id = id,
        editor = editor,
        workout = stored.map { it?.workout }.stateIn(viewModelScope, SharingStarted.Eagerly, null),
        past = past,
        userSettings = userSettings,
    )
    private val sets = SetHandler(session)
    private val exercises = ExerciseHandler(session, exerciseEditor)

    init {
        viewModelScope.launch { stored.collect { latest -> session.updateLocal { it.confirmedBy(latest?.workout) } } }
    }

    val uiState: StateFlow<WorkoutUiState> =
        combine(stored, past, userSettings, session.local) { stored, past, settings, local ->
            when {
                stored == null -> WorkoutUiState.Loading
                stored.workout == null -> WorkoutUiState.Gone
                else -> WorkoutUiMapper(settings, past, local).state(stored.workout)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutUiState.Loading)

    fun onAction(action: WorkoutAction) {
        when (action) {
            is WorkoutAction.OnSet -> sets.handle(action)

            is WorkoutAction.PadKey -> sets.press(action.key)

            is WorkoutAction.OnExercise -> exercises.handle(action)

            is WorkoutAction.OnWorkout -> exercises.handle(action)

            WorkoutAction.Undo -> {
                session.undo?.let { session.edit(it) }
                session.undo = null
                session.updateLocal { it.copy(message = null) }
            }

            WorkoutAction.DismissMessage -> session.updateLocal { it.copy(message = null) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
