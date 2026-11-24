package io.github.amibhai.liftlog.core.domain.usecase

import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.search.SearchIndex

/** Full-text matches, best first; when there are few, close spellings after them (SPEC §4). */
class SearchExercises(private val exercises: ExerciseRepository) {

    suspend operator fun invoke(
        query: String,
        index: SearchIndex,
        frecency: Map<ExerciseId, Double>,
    ): List<CatalogExercise> {
        val matches = index.rank(query, exercises.matching(query), frecency)
        val similar = if (matches.size < FEW) index.fuzzy(query, frecency, limit = FUZZY_LIMIT) else emptyList()
        return (matches + similar).distinctBy { it.id }
    }

    private companion object {
        const val FEW = 5
        const val FUZZY_LIMIT = 20
    }
}
