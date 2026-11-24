package io.github.amibhai.liftlog.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.RoutineSummary
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.program.nextDay
import io.github.amibhai.liftlog.core.domain.progress.WeekTotals
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.domain.progress.weeklyStreak
import io.github.amibhai.liftlog.core.domain.progress.weeklyTotals
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.time.daysAgo
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val exerciseCount: Int? = null,
    /** Starting another workout has to wait while this one is in progress. */
    val activeWorkoutId: WorkoutId? = null,
    /** The day of the followed program that is up next. */
    val next: NextWorkout? = null,
    val repeatLast: RepeatLast? = null,
    /** Null until loaded. */
    val routines: List<RoutineCardUiModel>? = null,
    /** Null until loaded, and before the first workout. */
    val training: HomeTraining? = null,
    /** A workout that was just started, for the screen to open. */
    val openWorkout: WorkoutId? = null,
    /** A routine that was just created, for its editor to open. */
    val openRoutine: RoutineId? = null,
    /** A routine that was just deleted and can still be brought back. */
    val deletedRoutine: RoutineId? = null,
)

/** This week and the latest records (SPEC §11). */
@Immutable
data class HomeTraining(
    val week: WeekTotals,
    val streakWeeks: Int,
    val records: List<RecordEntry>,
    val unit: WeightUnit,
)

/** The suggested next workout (SPEC §5, §7). */
@Immutable
data class NextWorkout(
    val programName: String,
    val dayName: String,
    val routineId: RoutineId,
    val exercises: List<String>,
)

/** "Repeat last Push" (SPEC §5): the routine of the last workout, or that workout when it had none. */
@Immutable
data class RepeatLast(val workoutId: WorkoutId, val routineId: RoutineId?, val routineName: String?)

@Immutable
data class RoutineCardUiModel(
    val id: RoutineId,
    val name: String,
    val exercises: List<String>,
    /** Null when it was never done. */
    val lastDoneDaysAgo: Int?,
)

internal data class HomeLocalState(
    val openWorkout: WorkoutId? = null,
    val openRoutine: RoutineId? = null,
    val deletedRoutine: RoutineId? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    exercises: ExerciseRepository,
    workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val startWorkout: StartWorkout,
    programs: ProgramRepository,
    private val progress: ProgressRepository,
    settings: SettingsRepository,
    private val ids: IdGenerator,
    private val work: SequentialWork,
    private val clock: Clock,
) : ViewModel() {

    private val local = MutableStateFlow(HomeLocalState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val training: Flow<HomeTraining?> = settings.settings.flatMapLatest { current ->
        combine(progress.observeWorkoutStats(), progress.observeRecentRecords(RECORDS)) { stats, records ->
            training(stats, records, current)
        }
    }

    private val base: Flow<HomeUiState> = combine(
        exercises.observeStats(),
        workouts.observeActive(),
        workouts.observeFinished(limit = 1).map { it.firstOrNull() },
        routines.observeAll(),
        local,
    ) { stats, active, last, routines, local ->
        HomeUiState(
            exerciseCount = stats.totalExercises,
            activeWorkoutId = active?.id,
            repeatLast = last?.let { repeatLast(it, routines) },
            routines = routines.map(::card),
            openWorkout = local.openWorkout,
            openRoutine = local.openRoutine,
            deletedRoutine = local.deletedRoutine,
        )
    }

    private val next: Flow<NextWorkout?> = combine(programs.observeActive(), routines.observeAll()) { active, all ->
        val day = active?.nextDay
        val routine = all.firstOrNull { it.routine.id == day?.routineId }?.routine
        if (active == null || day == null || routine == null) {
            null
        } else {
            NextWorkout(active.program.name, day.name, routine.id, routine.exercises.map { it.exercise.name })
        }
    }

    /** Starting a workout does not wait for the statistics. */
    val uiState: StateFlow<HomeUiState> =
        combine(base, next, training.onStart { emit(null) }) { state, next, training ->
            state.copy(next = next, training = training)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun startEmpty() = start { startWorkout.empty() }

    fun startRoutine(id: RoutineId) = start { startWorkout.fromRoutine(id) }

    fun repeatLast() {
        val repeat = uiState.value.repeatLast ?: return
        start { repeat.routineId?.let { startWorkout.fromRoutine(it) } ?: startWorkout.repeat(repeat.workoutId) }
    }

    fun onWorkoutOpened() = local.update { it.copy(openWorkout = null) }

    /** Creates an empty routine and then opens it; its editor deletes it again if it stays empty. */
    fun newRoutine() {
        val routine = Routine(RoutineId(ids.newId()), name = "")
        work.submit {
            routines.create(routine)
            local.update { it.copy(openRoutine = routine.id) }
        }
    }

    fun onRoutineOpened() = local.update { it.copy(openRoutine = null) }

    fun deleteRoutine(id: RoutineId) {
        work.submit { routines.delete(id) }
        local.update { it.copy(deletedRoutine = id) }
    }

    fun undoDeleteRoutine() {
        val id = local.value.deletedRoutine ?: return
        work.submit { routines.restore(id) }
        local.update { it.copy(deletedRoutine = null) }
    }

    fun onDeleteMessageShown() = local.update { it.copy(deletedRoutine = null) }

    private fun start(create: suspend () -> WorkoutId?) {
        viewModelScope.launch {
            val id = create() ?: return@launch
            local.update { it.copy(openWorkout = id) }
        }
    }

    private fun training(
        stats: List<WorkoutStats>,
        records: List<RecordEntry>,
        settings: UserSettings,
    ): HomeTraining? {
        if (stats.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(clock.withZone(zone))
        val firstDay = settings.weekStartsOn()
        return HomeTraining(
            week = weeklyTotals(stats, weeks = 1, today, firstDay, zone).single(),
            streakWeeks = weeklyStreak(stats.map { it.day(zone) }, today, firstDay),
            records = records,
            unit = settings.weightUnit,
        )
    }

    private fun repeatLast(last: Workout, routines: List<RoutineSummary>): RepeatLast {
        val routine = routines.firstOrNull { it.routine.id == last.routineId }?.routine
        return RepeatLast(last.id, routine?.id, routine?.name?.ifBlank { null })
    }

    private fun card(summary: RoutineSummary) = RoutineCardUiModel(
        id = summary.routine.id,
        name = summary.routine.name,
        exercises = summary.routine.exercises.map { it.exercise.name },
        lastDoneDaysAgo = summary.lastPerformedAt?.let { daysAgo(it, clock.millis(), ZoneId.systemDefault()) },
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val RECORDS = 3
    }
}
