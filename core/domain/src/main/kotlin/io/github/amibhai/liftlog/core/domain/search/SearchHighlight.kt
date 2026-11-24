package io.github.amibhai.liftlog.core.domain.search

import java.text.Normalizer

/** The parts of [name] to emphasize for [query]: the beginnings of words that match a typed word. */
fun highlight(name: String, query: String): List<IntRange> {
    val tokens = SearchText.tokens(query)
    if (tokens.isEmpty()) return emptyList()
    val folded = name.map(::fold)
    val ranges = mutableListOf<IntRange>()
    for (start in folded.indices) {
        val startsWord = folded[start] != null && (start == 0 || folded[start - 1] == null)
        if (!startsWord) continue
        val longest = tokens.filter { token -> token.indices.all { folded.getOrNull(start + it) == token[it] } }
            .maxOfOrNull { it.length }
        if (longest != null) ranges += start until start + longest
    }
    return ranges
}

/** One lowercase ASCII letter or digit per character, so that positions stay aligned; null for separators. */
private fun fold(char: Char): Char? =
    Normalizer.normalize(char.toString(), Normalizer.Form.NFKD).firstOrNull()?.lowercaseChar()
        ?.takeIf { it in 'a'..'z' || it in '0'..'9' }
