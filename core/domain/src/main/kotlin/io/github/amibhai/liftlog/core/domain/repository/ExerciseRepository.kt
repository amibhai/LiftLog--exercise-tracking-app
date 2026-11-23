package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSummary
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeStats(): Flow<LibraryStats>

    /** Prefix search over names, aliases and abbreviations. A blank query emits an empty list. */
    fun search(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): Flow<List<ExerciseSummary>>

    fun observeDetail(id: ExerciseId): Flow<ExerciseDetail?>

    fun observeSources(): Flow<List<LibrarySource>>

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 50
    }
}
