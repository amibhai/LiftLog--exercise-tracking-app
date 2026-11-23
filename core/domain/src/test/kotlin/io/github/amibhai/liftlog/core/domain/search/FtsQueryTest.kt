package io.github.amibhai.liftlog.core.domain.search

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class FtsQueryTest {

    @Test
    fun `every token becomes a prefix term`() {
        assertThat(FtsQuery.prefix("db bench")).isEqualTo("db* bench*")
    }

    @Test
    fun `hyphenated words are split like the FTS tokenizer splits them`() {
        assertThat(FtsQuery.prefix("pull-up")).isEqualTo("pull* up*")
    }

    @Test
    fun `blank or punctuation-only input has no query`() {
        assertThat(FtsQuery.prefix("")).isNull()
        assertThat(FtsQuery.prefix("   ")).isNull()
        assertThat(FtsQuery.prefix("\"*-:")).isNull()
    }

    @Test
    fun `FTS syntax in user input cannot change the query`() {
        assertThat(
            FtsQuery.prefix("bench OR \"squat\" NEAR name:press"),
        ).isEqualTo("bench* or* squat* near* name* press*")
    }

    @Test
    fun `very long input is capped`() {
        val query = FtsQuery.prefix((1..20).joinToString(" ") { "w$it" })

        assertThat(query!!.split(' ')).hasSize(FtsQuery.MAX_TOKENS)
    }
}
