package io.github.amibhai.liftlog.feature.exercises

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSummary
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeExerciseRepository(
        listOf(summary("Romanian Deadlift"), summary("Barbell Bench Press"), summary("Dumbbell Bench Press")),
    )

    @Test
    fun resultsArriveAfterTheDebounce() = runTest {
        val viewModel = ExercisesViewModel(repository)

        viewModel.uiState.test {
            assertThat(awaitItem().results).isEmpty()

            viewModel.onQueryChange("bench")
            advanceTimeBy(ExercisesViewModel.SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertThat(state.searchedQuery).isEqualTo("bench")
            assertThat(state.results.map { it.name }).containsExactly("Barbell Bench Press", "Dumbbell Bench Press")
            assertThat(state.totalExercises).isEqualTo(3)
        }
    }

    @Test
    fun fastTypingOnlySearchesTheLastQuery() = runTest {
        val viewModel = ExercisesViewModel(repository)

        viewModel.uiState.test {
            awaitItem()
            for (prefix in listOf("r", "ro", "rom")) {
                viewModel.onQueryChange(prefix)
                advanceTimeBy(ExercisesViewModel.SEARCH_DEBOUNCE_MS / 2)
            }
            advanceTimeBy(ExercisesViewModel.SEARCH_DEBOUNCE_MS + 1)

            assertThat(expectMostRecentItem().results.map { it.name }).containsExactly("Romanian Deadlift")
            assertThat(repository.queries.filter { it.isNotEmpty() }).containsExactly("rom")
        }
    }

    private fun summary(name: String) = ExerciseSummary(
        id = ExerciseId(name),
        name = name,
        type = ExerciseType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        primaryMuscle = Muscle.CHEST,
        thumbnailUrl = null,
        isCustom = false,
    )
}
