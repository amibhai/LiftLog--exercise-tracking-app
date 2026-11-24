package io.github.amibhai.liftlog.core.domain.usecase

import io.github.amibhai.liftlog.core.domain.model.ExerciseHistory
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.progress.plateau
import io.github.amibhai.liftlog.core.domain.progress.sessionProgress
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.util.concurrent.TimeUnit

/** "Bench has stalled for 3 weeks" (SPEC §7). [key] is the same until the lift moves again. */
data class PlateauInsight(val exerciseId: ExerciseId, val exerciseName: String, val weeks: Int, val key: String)

/** Gentle hints from the history; each can be put away. */
class Insights(
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** The most trained lifts first. */
    fun observe(): Flow<List<PlateauInsight>> {
        val since = clock.millis() - TimeUnit.DAYS.toMillis(DAYS)
        return combine(progress.observeLoadedExercises(since), settings.settings) { exercises, settings ->
            exercises.sortedByDescending { it.sessions.size }
                .mapNotNull { insight(it, settings) }
                .filter { it.key !in settings.dismissedInsights }
                .take(MOST)
        }
    }

    suspend fun dismiss(insight: PlateauInsight) =
        settings.update { it.copy(dismissedInsights = it.dismissedInsights + insight.key) }

    private fun insight(exercise: ExerciseHistory, settings: UserSettings): PlateauInsight? {
        val sessions = sessionProgress(exercise.type, exercise.sessions, settings.e1rmFormula)
        return plateau(sessions, clock.millis())?.let { plateau ->
            PlateauInsight(exercise.id, exercise.name, plateau.weeks, "plateau:${exercise.id.value}:${plateau.bestAt}")
        }
    }

    private companion object {
        const val DAYS = 182L
        const val MOST = 2
    }
}
