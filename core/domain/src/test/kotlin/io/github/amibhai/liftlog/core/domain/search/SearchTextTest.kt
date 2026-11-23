package io.github.amibhai.liftlog.core.domain.search

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SearchTextTest {

    @ParameterizedTest
    @CsvSource(
        "'Pull-Up', 'pull up'",
        "'  Bench   Press ', 'bench press'",
        "'Farmer''s Walk', 'farmer s walk'",
        "'Développé couché', 'developpe couche'",
        "'EZ-Bar Curl (21s)', 'ez bar curl 21s'",
        "'DB/BB', 'db bb'",
        "'', ''",
        "'!!!', ''",
    )
    fun `normalizes to lowercase ascii words`(input: String, expected: String) {
        assertThat(SearchText.normalize(input)).isEqualTo(expected)
    }
}
