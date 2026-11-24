package io.github.amibhai.liftlog.core.domain.search

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.Muscle
import org.junit.jupiter.api.Test

class SearchTermsTest {

    @Test
    fun `a custom exercise is found by its equipment and muscle words`() {
        val terms = SearchTerms.of(Equipment.DUMBBELL, listOf(Muscle.CHEST))

        assertThat(terms).containsAtLeast("db", "dumbbell", "chest")
    }

    @Test
    fun `terms are normalized, sorted and unique`() {
        for (equipment in Equipment.entries) {
            val terms = SearchTerms.of(equipment, Muscle.entries)

            assertThat(terms).isEqualTo(terms.map(SearchText::normalize).distinct().sorted())
        }
    }

    @Test
    fun `every muscle can be searched`() {
        for (muscle in Muscle.entries) {
            assertThat(SearchTerms.of(Equipment.OTHER, listOf(muscle))).isNotEmpty()
        }
    }
}
