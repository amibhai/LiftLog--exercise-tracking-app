package io.github.amibhai.liftlog.core.data.exercise

import io.github.amibhai.liftlog.core.data.di.ApplicationScope
import io.github.amibhai.liftlog.core.data.di.IoDispatcher
import io.github.amibhai.liftlog.core.data.workout.toDomain
import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.search.FtsQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
    @ApplicationScope scope: CoroutineScope,
    @IoDispatcher io: CoroutineDispatcher,
) : ExerciseRepository {

    /** Shared, because several screens need it and it takes a moment to load. */
    private val catalog: Flow<List<CatalogExercise>> =
        combine(dao.observeCatalog(), dao.observePrimaryMuscles(), dao.observeThumbnails(), ::toCatalog)
            .distinctUntilChanged()
            .flowOn(io)
            .shareIn(scope, SharingStarted.WhileSubscribed(KEEP_CATALOG_MS, replayExpirationMillis = 0), replay = 1)

    override fun observeStats(): Flow<LibraryStats> =
        combine(dao.observeActiveCount(), dao.observeCustomCount()) { total, custom ->
            LibraryStats(totalExercises = total, customExercises = custom)
        }

    override fun observeCatalog(): Flow<List<CatalogExercise>> = catalog

    override suspend fun matching(query: String): Set<ExerciseId> {
        val match = FtsQuery.prefix(query) ?: return emptySet()
        return dao.matchingIds(match).map(::ExerciseId).toSet()
    }

    override fun observeDetail(id: ExerciseId): Flow<ExerciseDetail?> =
        dao.observeWithDetails(id.value).map { it?.toDetail() }

    override suspend fun infos(ids: List<ExerciseId>): List<ExerciseInfo> {
        val found = dao.infos(ids.map { it.value }).associate { ExerciseId(it.id) to it.toDomain() }
        return ids.mapNotNull(found::get)
    }

    override fun observeSources(): Flow<List<LibrarySource>> =
        dao.observeSources().map { sources -> sources.map { it.toDomain() } }

    private companion object {
        const val KEEP_CATALOG_MS = 60_000L
    }
}
