package io.github.amibhai.liftlog.feature.widget

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.ThemeMode
import io.github.amibhai.liftlog.core.domain.program.nextDay
import io.github.amibhai.liftlog.core.domain.progress.weeklyStreak
import io.github.amibhai.liftlog.core.domain.progress.weeklyTotals
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.time.partOfDay
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** What the home screen widget shows (SPEC §11). */
@Immutable
internal data class WidgetState(
    /** The workout in progress. */
    val active: WorkoutTitle? = null,
    /** The day of the followed program that is up next. */
    val nextDay: String? = null,
    val streakWeeks: Int = 0,
    val workoutsThisWeek: Int = 0,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColor: Boolean = true,
)

/** The state of the widget as it changes. */
class WidgetStates @Inject constructor(
    private val workouts: WorkoutRepository,
    private val programs: ProgramRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    internal fun observe(): Flow<WidgetState> = combine(
        workouts.observeActive(),
        programs.observeActive(),
        progress.observeWorkoutStats(),
        settings.settings,
    ) { active, program, stats, settings ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(clock.withZone(zone))
        val firstDay = settings.weekStartsOn()
        WidgetState(
            active = active?.let { WorkoutTitle(it.name, partOfDay(it.startedAt, zone)) },
            nextDay = program?.nextDay?.name?.ifBlank { null },
            streakWeeks = weeklyStreak(stats.map { it.day(zone) }, today, firstDay),
            workoutsThisWeek = weeklyTotals(stats, weeks = 1, today, firstDay, zone).single().workouts,
            themeMode = settings.themeMode,
            useDynamicColor = settings.useDynamicColor,
        )
    }.distinctUntilChanged()
}
