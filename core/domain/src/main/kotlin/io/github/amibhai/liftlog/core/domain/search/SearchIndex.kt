package io.github.amibhai.liftlog.core.domain.search

import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.suggest.mostUsedFirst

/**
 * The exercise library prepared for ranking and typo-tolerant search (SPEC §4). Building it normalizes
 * every name once; keep it and reuse it for every query.
 */
class SearchIndex(val exercises: List<CatalogExercise>) {

    private class Entry(val exercise: CatalogExercise) {
        val name = SearchText.normalize(exercise.name)
        val nameWords = name.split(' ')
        val aliases = exercise.aliases.map(SearchText::normalize)
        val words = (listOf(name) + aliases + exercise.searchTerms).flatMap { it.split(' ') }.toSet()
    }

    private enum class Tier { EXACT_NAME, EXACT_ALIAS, PREFIX, WORD }

    private val prepared = exercises.map(::Entry)

    private val entriesByWord: Map<String, List<Entry>> = buildMap<String, MutableList<Entry>> {
        for (entry in prepared) for (word in entry.words) getOrPut(word) { mutableListOf() } += entry
    }

    /**
     * Orders [matches] (the ids the full-text search found): exact name, then exact alias or abbreviation,
     * then prefix, then any other word; equal matches by [frecency], popularity and name.
     */
    fun rank(query: String, matches: Set<ExerciseId>, frecency: Map<ExerciseId, Double>): List<CatalogExercise> {
        val text = SearchText.normalize(query)
        val words = text.split(' ')
        return prepared.filter { it.exercise.id in matches }
            .map { it.exercise to it.tier(text, words, isFamiliar(it.exercise, frecency)) }
            .sortedWith(
                compareBy<Pair<CatalogExercise, Tier>> { it.second }.thenBy(mostUsedFirst(frecency)) { it.first },
            )
            .map { it.first }
    }

    /** Exercises whose words are within a few typos of every word of [query], closest first. */
    fun fuzzy(query: String, frecency: Map<ExerciseId, Double>, limit: Int): List<CatalogExercise> {
        val tokens = SearchText.tokens(query)
        if (tokens.isEmpty()) return emptyList()
        val perToken = tokens.map(::closestEntries)
        return perToken.first().keys
            .filter { entry -> perToken.all { entry in it } }
            .map { entry -> entry.exercise to perToken.sumOf { it.getValue(entry) } }
            .sortedWith(
                compareBy<Pair<CatalogExercise, Int>> { it.second }.thenBy(mostUsedFirst(frecency)) { it.first },
            )
            .take(limit)
            .map { it.first }
    }

    /** Entries with a word close to [token], with the distance of their closest word. */
    private fun closestEntries(token: String): Map<Entry, Int> {
        val allowed = EditDistance.allowedFor(token)
        val closest = mutableMapOf<Entry, Int>()
        for ((word, owners) in entriesByWord) {
            val distance = EditDistance.toPrefixOf(word, token, allowed)
            if (distance <= allowed) owners.forEach { closest.merge(it, distance, ::minOf) }
        }
        return closest
    }

    private fun Entry.tier(text: String, queryWords: List<String>, isFamiliar: Boolean): Tier = when {
        name == text -> Tier.EXACT_NAME

        text in aliases || text in exercise.searchTerms -> Tier.EXACT_ALIAS

        name.startsWith(text) || aliases.any { it.startsWith(text) } -> Tier.PREFIX

        // "curl" means Barbell Curl to most people, so a known lift need not start with the word.
        isFamiliar && queryWords.all { query -> nameWords.any { it.startsWith(query) } } -> Tier.PREFIX

        else -> Tier.WORD
    }

    private fun isFamiliar(exercise: CatalogExercise, frecency: Map<ExerciseId, Double>): Boolean =
        exercise.popularityRank != null || exercise.id in frecency
}
