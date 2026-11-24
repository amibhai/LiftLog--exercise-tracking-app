package io.github.amibhai.liftlog.core.domain.search

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import org.junit.jupiter.api.Test

class SearchIndexTest {

    @Test
    fun `an exact name beats an exact alias, which beats a prefix, which beats a later word`() {
        val index = index(
            exercise("Front Squat"),
            exercise("Squat Jump"),
            exercise("Barbell Back Squat", aliases = listOf("Squat")),
            exercise("Squat"),
        )

        assertThat(index.ranked("squat"))
            .containsExactly("Squat", "Barbell Back Squat", "Squat Jump", "Front Squat").inOrder()
    }

    @Test
    fun `an abbreviation counts as an alias`() {
        val index = index(
            exercise("OHP Machine"),
            exercise("Barbell Overhead Press", terms = listOf("bb", "ohp"), popularity = 4),
        )

        assertThat(index.ranked("ohp")).containsExactly("Barbell Overhead Press", "OHP Machine").inOrder()
    }

    @Test
    fun `part of a search term is not an exact match`() {
        val index = index(exercise("Push-Up", terms = listOf("press up")), exercise("Press Sit-Up"))

        assertThat(index.ranked("press")).containsExactly("Press Sit-Up", "Push-Up").inOrder()
    }

    @Test
    fun `hyphens and case do not matter`() {
        val index = index(exercise("Archer Pull Up"), exercise("Pull-Up"))

        assertThat(index.ranked("PULL up").first()).isEqualTo("Pull-Up")
    }

    @Test
    fun `a well-known lift counts as a prefix match wherever its name contains the word`() {
        val index = index(
            exercise("Curl with Shoulder Elevated"),
            exercise("Barbell Curl", popularity = 17),
            exercise("Zottman Curl"),
        )

        assertThat(index.ranked("curl"))
            .containsExactly("Barbell Curl", "Curl with Shoulder Elevated", "Zottman Curl").inOrder()
    }

    @Test
    fun `what I do most comes first among equal matches`() {
        val cable = exercise("Cable Curl", popularity = 5)
        val zottman = exercise("Zottman Curl")
        val index = index(cable, zottman, exercise("Curl Machine"))

        val frecency = mapOf(zottman.id to 3.0)

        assertThat(index.ranked("curl", frecency))
            .containsExactly("Zottman Curl", "Cable Curl", "Curl Machine").inOrder()
    }

    @Test
    fun `typos still find the exercise`() {
        val index = index(
            exercise("EZ-Bar Skullcrusher"),
            exercise("Romanian Deadlift", terms = listOf("rdl")),
            exercise("Dumbbell Bench Press"),
            exercise("Bulgarian Split Squat"),
        )

        assertThat(index.fuzzyNames("skulcrusher")).containsExactly("EZ-Bar Skullcrusher")
        assertThat(index.fuzzyNames("romainian")).containsExactly("Romanian Deadlift")
        assertThat(index.fuzzyNames("dumbell bnech")).containsExactly("Dumbbell Bench Press")
        assertThat(index.fuzzyNames("bulgrian spl")).containsExactly("Bulgarian Split Squat")
        assertThat(index.fuzzyNames("xylophone")).isEmpty()
    }

    @Test
    fun `the closest typo match comes first`() {
        val index = index(exercise("Bench Pull"), exercise("Beach Press"), exercise("Bend Press"))

        assertThat(index.fuzzyNames("beanch press").first()).isEqualTo("Beach Press")
    }

    @Test
    fun `short words must be typed correctly`() {
        assertThat(index(exercise("Dip"), exercise("Row")).fuzzyNames("dop")).isEmpty()
    }

    @Test
    fun `matches are highlighted at the start of words`() {
        assertThat(highlight("Barbell Bench Press", "bench pr")).containsExactly(8..12, 14..15).inOrder()
        assertThat(highlight("Pull-Up", "pull up")).containsExactly(0..3, 5..6).inOrder()
        assertThat(highlight("Développé Couché", "devel")).containsExactly(0..4)
        assertThat(highlight("Barbell Curl", "rdl")).isEmpty()
        assertThat(highlight("Bench", "be ben")).containsExactly(0..2)
    }

    @Test
    fun `fuzzy matching stays fast on a full-size library`() {
        val words =
            listOf("barbell", "dumbbell", "bench", "press", "squat", "curl", "raise", "row", "lunge", "extension")
        val library = (0 until 2_000).map { i ->
            exercise("${words[i % 10]} ${words[(i / 10) % 10]} ${words[(i / 100) % 10]} $i")
        }
        val index = SearchIndex(library)

        val start = System.nanoTime()
        repeat(20) { index.fuzzy("dumbell bnech prss", emptyMap(), limit = 20) }
        val millisPerQuery = (System.nanoTime() - start) / 20 / 1_000_000

        assertThat(millisPerQuery).isLessThan(50)
    }

    private fun SearchIndex.ranked(query: String, frecency: Map<ExerciseId, Double> = emptyMap()) =
        rank(query, exercises.map { it.id }.toSet(), frecency).map { it.name }

    private fun SearchIndex.fuzzyNames(query: String) = fuzzy(query, emptyMap(), limit = 10).map { it.name }

    private fun index(vararg exercises: CatalogExercise) = SearchIndex(exercises.toList())

    private fun exercise(
        name: String,
        aliases: List<String> = emptyList(),
        terms: List<String> = emptyList(),
        popularity: Int? = null,
    ) = CatalogExercise(
        id = ExerciseId(name),
        name = name,
        aliases = aliases,
        searchTerms = terms,
        type = ExerciseType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        popularityRank = popularity,
    )
}
