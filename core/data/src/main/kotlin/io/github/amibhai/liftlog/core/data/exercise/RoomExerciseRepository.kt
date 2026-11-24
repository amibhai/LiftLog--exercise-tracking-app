package io.github.amibhai.liftlog.core.data.exercise

import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSummary
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.search.FtsQuery
import io.github.amibhai.liftlog.core.domain.search.SearchText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class RoomExerciseRepository @Inject constructor(private val dao: ExerciseDao) : ExerciseRepository {

    override fun observeStats(): Flow<LibraryStats> =
        combine(dao.observeActiveCount(), dao.observeCustomCount()) { total, custom ->
            LibraryStats(totalExercises = total, customExercises = custom)
        }

    override fun search(query: String, limit: Int): Flow<List<ExerciseSummary>> {
        val match = FtsQuery.prefix(query) ?: return flowOf(emptyList())
        return dao.search(match = match, query = SearchText.normalize(query), limit = limit)
            .map { rows -> rows.map { it.toSummary() } }
    }

    override fun observeDetail(id: ExerciseId): Flow<ExerciseDetail?> =
        dao.observeWithDetails(id.value).map { it?.toDetail() }

    override fun observeSources(): Flow<List<LibrarySource>> =
        dao.observeSources().map { sources -> sources.map { it.toDomain() } }
}
