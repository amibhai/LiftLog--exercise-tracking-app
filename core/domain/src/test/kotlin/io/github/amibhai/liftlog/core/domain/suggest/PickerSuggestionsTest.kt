package io.github.amibhai.liftlog.core.domain.suggest

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

class PickerSuggestionsTest {

    private val now = 1_700_000_000_000L

    private val catalog = ('a'..'z').map { letter ->
        CatalogExercise(
            id = ExerciseId(letter.toString()),
            name = letter.toString(),
            type = ExerciseType.WEIGHT_REPS,
            equipment = if (letter < 'm') Equipment.BARBELL else Equipment.CABLE,
            primaryMuscles = listOf(if (letter in "aeiou") Muscle.CHEST else Muscle.LATS),
            popularityRank = (letter - 'a' + 1).takeIf { it <= 20 },
            isFavorite = letter in "xz",
        )
    }

    @Test
    fun `a new user sees the starter lifts and nothing else`() {
        val suggestions = build(emptyList())

        assertThat(suggestions.starters).hasSize(12)
        assertThat(suggestions.starters.first()).isEqualTo(ExerciseId("a"))
        assertThat(suggestions.frequent).isEmpty()
        assertThat(suggestions.recent).isEmpty()
        assertThat(suggestions.favorites.map { it.value }).containsExactly("x", "z").inOrder()
    }

    @Test
    fun `the eight most frecent exercises come first`() {
        val history = ('a'..'j').flatMapIndexed { index, letter ->
            (0..index).map { day -> used(letter, "w$letter$day", daysAgo = day) }
        }

        val suggestions = build(history)

        assertThat(
            suggestions.frequent.map { it.value },
        ).containsExactly("j", "i", "h", "g", "f", "e", "d", "c").inOrder()
        assertThat(suggestions.starters).isEmpty()
    }

    @Test
    fun `recent lists the last distinct exercises that are not already shown`() {
        val habit = ('a'..'h').flatMap { letter -> (1..5).map { used(letter, "h$it", daysAgo = 10 + it) } }
        val lately = listOf(used('q', "w1", 1), used('r', "w2", 2), used('q', "w3", 3), used('a', "w1", 1))

        assertThat(build(habit + lately).recent.map { it.value }).containsExactly("q", "r").inOrder()
    }

    @Test
    fun `exercises done together with the current ones are suggested`() {
        val history = listOf(
            used('b', "push1", 3),
            used('c', "push1", 3),
            used('b', "push2", 6),
            used('c', "push2", 6),
            used('d', "push2", 6),
            used('k', "legs", 2),
        )

        val suggestions = build(history, current = setOf("b"))

        assertThat(suggestions.goesWith.map { it.value }).containsExactly("c", "d").inOrder()
    }

    @Test
    fun `exercises already added are not suggested again`() {
        val history = listOf(used('b', "push", 1), used('c', "push", 1))

        val suggestions = build(history, current = setOf("b", "c"))

        assertThat(suggestions.frequent).isEmpty()
        assertThat(suggestions.goesWith).isEmpty()
        assertThat(suggestions.recent).isEmpty()
    }

    @Test
    fun `hidden exercises are not suggested`() {
        val history = listOf(ExerciseUsage(ExerciseId("gone"), WorkoutId("w"), now), used('c', "w", 1))

        assertThat(build(history).frequent.map { it.value }).containsExactly("c")
    }

    @Test
    fun `browsing filters by muscle and equipment, most used first`() {
        val frecency = mapOf(ExerciseId("i") to 2.0)

        val chest = browse(catalog, muscle = Muscle.CHEST, equipment = null, frecency = frecency)
        assertThat(chest.map { it.name }).containsExactly("i", "a", "e", "o", "u").inOrder()

        val chestCable = browse(catalog, muscle = Muscle.CHEST, equipment = Equipment.CABLE, frecency = frecency)
        assertThat(chestCable.map { it.name }).containsExactly("o", "u").inOrder()
    }

    private fun build(history: List<ExerciseUsage>, current: Set<String> = emptySet()) =
        PickerSuggestions.build(catalog, history, current.map(::ExerciseId).toSet(), now)

    private fun used(letter: Char, workout: String, daysAgo: Int) =
        ExerciseUsage(ExerciseId(letter.toString()), WorkoutId(workout), now - daysAgo.days.inWholeMilliseconds)
}
