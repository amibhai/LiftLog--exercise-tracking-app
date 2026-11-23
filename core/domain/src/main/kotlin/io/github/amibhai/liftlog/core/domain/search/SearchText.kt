package io.github.amibhai.liftlog.core.domain.search

import java.text.Normalizer
import java.util.Locale

/**
 * Search normalization shared by queries and stored search text: lowercase ASCII words.
 * Mirrors `search_normalize` in tools/exercise-pipeline/pipeline/normalize.py.
 */
object SearchText {
    private val nonAscii = Regex("[^\\p{ASCII}]")
    private val separators = Regex("[^a-z0-9]+")

    fun normalize(input: String): String {
        val folded = Normalizer.normalize(input, Normalizer.Form.NFKD).replace(nonAscii, "")
        return folded.lowercase(Locale.ROOT).replace(separators, " ").trim()
    }

    fun tokens(input: String): List<String> = normalize(input).split(' ').filter { it.isNotEmpty() }
}
