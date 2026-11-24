package io.github.amibhai.liftlog.core.testing

import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSummary
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.search.SearchText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory ExerciseRepository: name-contains search, and a record of every query made. */
class FakeExerciseRepository(
    private val exercises: List<ExerciseSummary> = emptyList(),
    private val details: Map<ExerciseId, ExerciseDetail> = emptyMap(),
    private val sources: List<LibrarySource> = emptyList(),
) : ExerciseRepository {
    val queries = mutableListOf<String>()

    override fun observeStats(): Flow<LibraryStats> =
        flowOf(LibraryStats(totalExercises = exercises.size, customExercises = exercises.count { it.isCustom }))

    override fun search(query: String, limit: Int): Flow<List<ExerciseSummary>> {
        queries += query
        val normalized = SearchText.normalize(query)
        if (normalized.isEmpty()) return flowOf(emptyList())
        return flowOf(exercises.filter { normalized in SearchText.normalize(it.name) }.take(limit))
    }

    override fun observeDetail(id: ExerciseId): Flow<ExerciseDetail?> = flowOf(details[id])

    override fun observeSources(): Flow<List<LibrarySource>> = flowOf(sources)
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = state.map { it }

    val current: UserSettings get() = state.value

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        state.update(transform)
    }
}
