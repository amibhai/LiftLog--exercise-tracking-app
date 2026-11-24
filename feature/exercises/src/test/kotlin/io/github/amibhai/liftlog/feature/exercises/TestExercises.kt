package io.github.amibhai.liftlog.feature.exercises

import io.github.amibhai.liftlog.core.domain.concurrent.AppDispatchers
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.usecase.SearchExercises
import io.github.amibhai.liftlog.core.testing.FakeHistoryRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseBrowser
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseRowUiModel
import kotlinx.coroutines.CoroutineDispatcher
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal const val NOW = 1_700_000_000_000L

internal val testClock: Clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)

internal fun catalogExercise(
    name: String,
    muscle: Muscle = Muscle.CHEST,
    equipment: Equipment = Equipment.BARBELL,
    popularityRank: Int? = null,
    isFavorite: Boolean = false,
) = CatalogExercise(
    id = ExerciseId(name),
    name = name,
    type = ExerciseType.WEIGHT_REPS,
    equipment = equipment,
    primaryMuscles = listOf(muscle),
    popularityRank = popularityRank,
    isFavorite = isFavorite,
)

internal fun browser(
    exercises: ExerciseRepository,
    dispatcher: CoroutineDispatcher,
    history: HistoryRepository = FakeHistoryRepository(),
    settings: SettingsRepository = FakeSettingsRepository(),
) = ExerciseBrowser(exercises, history, settings, SearchExercises(exercises), testClock, AppDispatchers(dispatcher))

internal fun row(name: String) = ExerciseRowUiModel(
    id = ExerciseId(name),
    name = name,
    equipment = Equipment.BARBELL,
    muscle = Muscle.CHEST,
    thumbnailUrl = null,
)
