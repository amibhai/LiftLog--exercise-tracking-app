package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeStats(): Flow<LibraryStats>

    /** Every exercise that is not hidden, for ranking, browsing and suggestions. */
    fun observeCatalog(): Flow<List<CatalogExercise>>

    /** Full-text prefix search over names, aliases and abbreviations, unranked. Empty for a blank query. */
    suspend fun matching(query: String): Set<ExerciseId>

    fun observeDetail(id: ExerciseId): Flow<ExerciseDetail?>

    /** What logging needs to know, in the order of [ids]; unknown ids are left out. */
    suspend fun infos(ids: List<ExerciseId>): List<ExerciseInfo>

    fun observeSources(): Flow<List<LibrarySource>>
}
