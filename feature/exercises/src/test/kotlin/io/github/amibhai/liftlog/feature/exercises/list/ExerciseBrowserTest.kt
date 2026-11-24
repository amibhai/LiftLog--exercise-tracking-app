package io.github.amibhai.liftlog.feature.exercises.list

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.FakeHistoryRepository
import io.github.amibhai.liftlog.feature.exercises.NOW
import io.github.amibhai.liftlog.feature.exercises.browser
import io.github.amibhai.liftlog.feature.exercises.catalogExercise
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseBrowserTest {

    private val exercises = FakeExerciseRepository(
        listOf(
            catalogExercise("Barbell Bench Press", popularityRank = 1),
            catalogExercise("Dumbbell Bench Press", equipment = Equipment.DUMBBELL),
            catalogExercise("Barbell Row", muscle = Muscle.UPPER_BACK, popularityRank = 5),
        ),
    )
    private val filter = MutableStateFlow(ExerciseFilter())
    private val current = MutableStateFlow(emptySet<ExerciseId>())

    @Test
    fun aNewUserSeesThePopularLifts() = runTest {
        states().test {
            val state = awaitItem()
            assertThat(state.totalExercises).isEqualTo(3)
            assertThat(state.sections.single().kind).isEqualTo(ExerciseSectionKind.STARTERS)
            assertThat(state.sections.single().rows.map { it.name })
                .containsExactly("Barbell Bench Press", "Barbell Row").inOrder()
        }
    }

    @Test
    fun rowsShowWhatWasDoneLast() = runTest {
        val bench = ExerciseId("Barbell Bench Press")
        val at = NOW - 3.days.inWholeMilliseconds
        val sets = listOf(
            LoggedSet(SetType.WARMUP, SetValues(Weight(100.0), reps = 1)),
            LoggedSet(SetType.WORKING, SetValues(Weight(80.0), reps = 8)),
        )
        val history = FakeHistoryRepository(
            usages = listOf(ExerciseUsage(bench, WorkoutId("w"), at)),
            sessions = mapOf(bench to listOf(ExerciseSession(WorkoutId("w"), at, sets))),
        )

        states(history).test {
            val row = awaitItem().sections.first().rows.single()
            assertThat(row.name).isEqualTo("Barbell Bench Press")
            assertThat(row.lastSet).isEqualTo("80 kg × 8")
            assertThat(row.lastDaysAgo).isEqualTo(3)
        }
    }

    @Test
    fun typingSearchesAfterTheDebounceAndHighlightsMatches() = runTest {
        states().test {
            awaitItem()
            for (text in listOf("b", "be", "bench")) {
                filter.value = ExerciseFilter(query = text)
                advanceTimeBy(ExerciseBrowser.SEARCH_DEBOUNCE_MS / 2)
            }
            advanceTimeBy(ExerciseBrowser.SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertThat(state.filter.query).isEqualTo("bench")
            assertThat(state.sections.single().rows.map { it.name })
                .containsExactly("Barbell Bench Press", "Dumbbell Bench Press").inOrder()
            assertThat(state.sections.single().rows.first().highlights).containsExactly(8..12)
            assertThat(exercises.queries).containsExactly("bench")
        }
    }

    @Test
    fun aTypoStillFindsTheExercise() = runTest {
        states().test {
            awaitItem()
            filter.value = ExerciseFilter(query = "dumbell")
            advanceTimeBy(ExerciseBrowser.SEARCH_DEBOUNCE_MS + 1)

            assertThat(expectMostRecentItem().sections.single().rows.map { it.name })
                .containsExactly("Dumbbell Bench Press")
        }
    }

    @Test
    fun chipsBrowseWithoutTyping() = runTest {
        states().test {
            awaitItem()
            filter.value = ExerciseFilter(muscle = Muscle.CHEST)
            assertThat(awaitItem().sections.single().rows).hasSize(2)

            filter.value = ExerciseFilter(muscle = Muscle.CHEST, equipment = Equipment.DUMBBELL)
            assertThat(awaitItem().sections.single().rows.map { it.name }).containsExactly("Dumbbell Bench Press")
        }
    }

    @Test
    fun creatingIsOfferedUnlessTheNameExists() = runTest {
        states().test {
            assertThat(awaitItem().canCreate).isFalse()

            filter.value = ExerciseFilter(query = "Landmine Press")
            advanceTimeBy(ExerciseBrowser.SEARCH_DEBOUNCE_MS + 1)
            assertThat(expectMostRecentItem().canCreate).isTrue()

            filter.value = ExerciseFilter(query = "barbell ROW")
            advanceTimeBy(ExerciseBrowser.SEARCH_DEBOUNCE_MS + 1)
            assertThat(expectMostRecentItem().canCreate).isFalse()
        }
    }

    @Test
    fun exercisesOfTheWorkoutAreMarkedAndNotSuggested() = runTest {
        current.value = setOf(ExerciseId("Barbell Row"))

        states().test {
            assertThat(awaitItem().sections.single().rows.map { it.name }).containsExactly("Barbell Bench Press")

            filter.value = ExerciseFilter(muscle = Muscle.UPPER_BACK)
            assertThat(awaitItem().sections.single().rows.single().isAdded).isTrue()
        }
    }

    private fun TestScope.states(history: FakeHistoryRepository = FakeHistoryRepository()) =
        browser(exercises, UnconfinedTestDispatcher(testScheduler), history).uiState(filter, current)
}
