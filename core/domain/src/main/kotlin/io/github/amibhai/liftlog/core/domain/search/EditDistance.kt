package io.github.amibhai.liftlog.core.domain.search

/** Damerau-Levenshtein distance: insertions, deletions, substitutions and swapped neighbours. */
internal object EditDistance {
    private const val SHORT = 3
    private const val MEDIUM = 5

    /** Typos allowed in a word: none in short ones, where every typo is another word. */
    fun allowedFor(token: String): Int = when {
        token.length <= SHORT -> 0
        token.length <= MEDIUM -> 1
        else -> 2
    }

    /**
     * Distance from [typed] to the closest prefix of [word], so that a word still being typed matches.
     * Distances above [limit] may come out as any number above it.
     */
    fun toPrefixOf(word: String, typed: String, limit: Int): Int = when {
        limit == 0 -> if (word.startsWith(typed)) 0 else 1
        typed.length - word.length > limit -> limit + 1
        else -> distance(word.take(typed.length + limit), typed, limit)
    }

    private fun distance(word: String, typed: String, limit: Int): Int {
        val width = word.length
        val rows = Array(typed.length + 1) { IntArray(width + 1) }
        for (j in 0..width) rows[0][j] = j
        for (i in 1..typed.length) {
            rows[i][0] = i
            for (j in 1..width) rows[i][j] = cell(rows, word, typed, i, j)
            if (rows[i].min() > limit) return limit + 1
        }
        return rows[typed.length].min()
    }

    private fun cell(rows: Array<IntArray>, word: String, typed: String, i: Int, j: Int): Int {
        val substitution = rows[i - 1][j - 1] + if (typed[i - 1] == word[j - 1]) 0 else 1
        val distance = minOf(rows[i - 1][j] + 1, rows[i][j - 1] + 1, substitution)
        val swapped = i > 1 && j > 1 && typed[i - 1] == word[j - 2] && typed[i - 2] == word[j - 1]
        return if (swapped) minOf(distance, rows[i - 2][j - 2] + 1) else distance
    }
}
