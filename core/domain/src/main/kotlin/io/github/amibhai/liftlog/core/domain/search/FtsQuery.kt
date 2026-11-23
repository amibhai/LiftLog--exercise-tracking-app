package io.github.amibhai.liftlog.core.domain.search

/**
 * Builds FTS4 `MATCH` expressions from what the user typed (SPEC §4).
 *
 * Input is reduced to lowercase alphanumeric tokens, so FTS operators, quotes and column filters
 * typed by the user are plain search words and can never change the query.
 */
object FtsQuery {
    const val MAX_TOKENS = 8

    /** Every token must prefix-match some indexed word ("db bench" -> "db* bench*"); null if blank. */
    fun prefix(input: String): String? {
        val tokens = SearchText.tokens(input).take(MAX_TOKENS)
        return tokens.takeIf { it.isNotEmpty() }?.joinToString(separator = " ") { "$it*" }
    }
}
