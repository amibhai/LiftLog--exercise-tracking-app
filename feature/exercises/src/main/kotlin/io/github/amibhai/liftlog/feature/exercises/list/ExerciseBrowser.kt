package io.github.amibhai.liftlog.feature.exercises.list

import io.github.amibhai.liftlog.core.domain.concurrent.AppDispatchers
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.search.SearchIndex
import io.github.amibhai.liftlog.core.domain.search.SearchText
import io.github.amibhai.liftlog.core.domain.search.highlight
import io.github.amibhai.liftlog.core.domain.suggest.Frecency
import io.github.amibhai.liftlog.core.domain.suggest.PickerSuggestions
import io.github.amibhai.liftlog.core.domain.suggest.browse
import io.github.amibhai.liftlog.core.domain.time.daysAgo
import io.github.amibhai.liftlog.core.domain.usecase.SearchExercises
import io.github.amibhai.liftlog.core.domain.workout.topSet
import io.github.amibhai.liftlog.core.ui.format.SetText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/**
 * Suggestions, browsing and search over the exercise library (SPEC §4), shared by the Exercises tab and
 * the picker.
 */
class ExerciseBrowser @Inject constructor(
    private val exercises: ExerciseRepository,
    private val history: HistoryRepository,
    private val settings: SettingsRepository,
    private val search: SearchExercises,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) {
    /** The library together with what I did with it. */
    private class Library(
        val index: SearchIndex,
        val usages: List<ExerciseUsage>,
        val lastSessions: Map<ExerciseId, ExerciseSession>,
        val now: Long,
    ) {
        val byId = index.exercises.associateBy { it.id }
        val frecency = Frecency.scores(usages, now)
    }

    private class Request(
        val library: Library,
        val filter: ExerciseFilter,
        val current: Set<ExerciseId>,
        val settings: UserSettings,
    )

    /** [current] are the exercises of the workout or routine that is being added to. */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun uiState(filter: Flow<ExerciseFilter>, current: Flow<Set<ExerciseId>>): Flow<ExerciseListUiState> {
        val settled = filter.debounce { if (it.query.isBlank()) 0 else SEARCH_DEBOUNCE_MS }
        return combine(library(), settled, current, settings.settings, ::Request)
            .mapLatest(::build)
            .flowOn(dispatchers.default)
    }

    private fun library(): Flow<Library> = combine(exercises.observeCatalog(), past()) { catalog, (usages, last, now) ->
        Library(SearchIndex(catalog), usages, last, now)
    }

    private fun past(): Flow<Triple<List<ExerciseUsage>, Map<ExerciseId, ExerciseSession>, Long>> = flow {
        val now = clock.millis()
        val since = now - Frecency.HORIZON.inWholeMilliseconds
        emit(Triple(history.usages(since), history.latestSessions(before = now), now))
    }

    private suspend fun build(request: Request): ExerciseListUiState {
        val results = if (request.filter.isBlank) emptyList() else find(request)
        val name = SearchText.normalize(request.filter.query)
        return ExerciseListUiState(
            isLoading = false,
            totalExercises = request.library.byId.size,
            filter = request.filter,
            sections = if (request.filter.isBlank) {
                suggestions(request)
            } else {
                listOf(ExerciseSectionUiModel(ExerciseSectionKind.RESULTS, results.map { row(it, request) }))
            },
            canCreate = name.isNotEmpty() && results.none { SearchText.normalize(it.name) == name },
        )
    }

    private suspend fun find(request: Request): List<CatalogExercise> {
        val filter = request.filter
        val frecency = request.library.frecency
        val found = if (filter.query.isBlank()) {
            request.library.index.exercises
        } else {
            search(filter.query, request.library.index, frecency)
        }
        return if (filter.query.isBlank()) {
            browse(found, filter.muscle, filter.equipment, frecency)
        } else {
            found.filter { filter.muscle == null || filter.muscle in it.primaryMuscles }
                .filter { filter.equipment == null || it.equipment == filter.equipment }
                .take(MAX_RESULTS)
        }
    }

    private fun suggestions(request: Request): List<ExerciseSectionUiModel> {
        val library = request.library
        val suggested =
            PickerSuggestions.build(library.index.exercises, library.usages, request.current, library.now)
        return listOf(
            ExerciseSectionKind.FREQUENT to suggested.frequent,
            ExerciseSectionKind.GOES_WITH to suggested.goesWith,
            ExerciseSectionKind.RECENT to suggested.recent,
            ExerciseSectionKind.FAVORITES to suggested.favorites,
            ExerciseSectionKind.STARTERS to suggested.starters,
        ).filter { (_, ids) -> ids.isNotEmpty() }.map { (kind, ids) ->
            ExerciseSectionUiModel(kind, ids.mapNotNull(library.byId::get).map { row(it, request) })
        }
    }

    private fun row(exercise: CatalogExercise, request: Request): ExerciseRowUiModel {
        val session = request.library.lastSessions[exercise.id]
        val best = session?.let { topSet(it.sets, exercise.type) }
        return ExerciseRowUiModel(
            id = exercise.id,
            name = exercise.name,
            equipment = exercise.equipment,
            muscle = exercise.primaryMuscles.firstOrNull(),
            thumbnailUrl = exercise.thumbnailUrl,
            highlights = highlight(exercise.name, request.filter.query),
            lastSet = best?.let { SetText(request.settings.weightUnit).summary(exercise.type, it.values) },
            lastDaysAgo = session?.let { daysAgo(it.at, request.library.now, ZoneId.systemDefault()) } ?: 0,
            isAdded = exercise.id in request.current,
        )
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 100L
        private const val MAX_RESULTS = 100
    }
}
