package io.github.amibhai.liftlog.core.testing

import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.search.SearchText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory ExerciseRepository: name-contains search, and a record of every query made. */
class FakeExerciseRepository(
    catalog: List<CatalogExercise> = emptyList(),
    details: Map<ExerciseId, ExerciseDetail> = emptyMap(),
    private val sources: List<LibrarySource> = emptyList(),
) : ExerciseRepository {
    val queries = mutableListOf<String>()
    val catalog = MutableStateFlow(catalog)
    val details = MutableStateFlow(details)

    override fun observeStats(): Flow<LibraryStats> =
        catalog.map { all -> LibraryStats(totalExercises = all.size, customExercises = all.count { it.isCustom }) }

    override fun observeCatalog(): Flow<List<CatalogExercise>> = catalog

    override suspend fun matching(query: String): Set<ExerciseId> {
        queries += query
        val normalized = SearchText.normalize(query)
        if (normalized.isEmpty()) return emptySet()
        return catalog.value.filter { normalized in SearchText.normalize(it.name) }.map { it.id }.toSet()
    }

    override fun observeDetail(id: ExerciseId): Flow<ExerciseDetail?> = details.map { it[id] }

    override suspend fun infos(ids: List<ExerciseId>): List<ExerciseInfo> = ids.mapNotNull { id ->
        catalog.value.firstOrNull { it.id == id }?.let { ExerciseInfo(it.id, it.name, it.type, it.equipment) }
    }

    override fun observeSources(): Flow<List<LibrarySource>> = flowOf(sources)
}

/** Records what was asked of it. */
class FakeExerciseEditor : ExerciseEditor {
    val saved = mutableListOf<ExerciseDraft>()
    val deleted = mutableListOf<ExerciseId>()
    val hidden = mutableMapOf<ExerciseId, Boolean>()
    val favorites = mutableMapOf<ExerciseId, Boolean>()
    val notes = mutableMapOf<ExerciseId, String?>()
    val defaults = mutableMapOf<ExerciseId, ExerciseDefaults>()

    override suspend fun save(draft: ExerciseDraft): ExerciseId {
        saved += draft
        return draft.id ?: ExerciseId("new-${saved.size}")
    }

    override suspend fun deleteCustom(id: ExerciseId) {
        deleted += id
    }

    override suspend fun setHidden(id: ExerciseId, hidden: Boolean) {
        this.hidden[id] = hidden
    }

    override suspend fun setFavorite(id: ExerciseId, favorite: Boolean) {
        favorites[id] = favorite
    }

    override suspend fun setStickyNote(id: ExerciseId, note: String?) {
        notes[id] = note
    }

    override suspend fun setDefaults(id: ExerciseId, defaults: ExerciseDefaults) {
        this.defaults[id] = defaults
    }
}

class FakeHistoryRepository(
    var usages: List<ExerciseUsage> = emptyList(),
    sessions: Map<ExerciseId, List<ExerciseSession>> = emptyMap(),
) : HistoryRepository {
    val sessions = MutableStateFlow(sessions)

    override suspend fun usages(since: Long): List<ExerciseUsage> = usages.filter { it.at >= since }

    override suspend fun latestSessions(before: Long): Map<ExerciseId, ExerciseSession> = buildMap {
        for ((id, all) in sessions.value) all.filter { it.at < before }.maxByOrNull { it.at }?.let { put(id, it) }
    }

    override suspend fun latestSessions(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, ExerciseSession> =
        latestSessions(before).filterKeys { it in ids }

    override fun observeSessions(id: ExerciseId, limit: Int): Flow<List<ExerciseSession>> =
        sessions.map { all -> all[id].orEmpty().sortedByDescending { it.at }.take(limit) }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = state.map { it }

    val current: UserSettings get() = state.value

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        state.update(transform)
    }
}
