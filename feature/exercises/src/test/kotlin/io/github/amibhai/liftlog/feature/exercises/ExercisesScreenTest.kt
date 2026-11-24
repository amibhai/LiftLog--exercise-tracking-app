package io.github.amibhai.liftlog.feature.exercises

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.ui.theme.LiftLogTheme
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseFilter
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseListUiState
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseSectionKind
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseSectionUiModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExercisesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var state by mutableStateOf(ExerciseListUiState(isLoading = false, totalExercises = 1_670))
    private val opened = mutableListOf<String>()
    private val created = mutableListOf<String>()

    @Test
    fun theSearchFieldTellsTheLibrarySize() {
        show(query = "")

        compose.onNodeWithText("Search 1,670 exercises").assertIsDisplayed()
    }

    @Test
    fun suggestionsAreTitledAndOpenOnTap() {
        state = state.copy(sections = listOf(section(ExerciseSectionKind.FREQUENT, "Barbell Bench Press")))
        show(query = "")

        compose.onNodeWithText("Frequently done").assertIsDisplayed()
        compose.onNodeWithText("Barbell Bench Press").performClick()

        assertThat(opened).containsExactly("Barbell Bench Press")
    }

    @Test
    fun aQueryWithoutResultsOffersToCreateTheExercise() {
        state = state.copy(filter = ExerciseFilter(query = "xyz"), canCreate = true)
        show(query = "xyz")

        compose.onNodeWithText("Nothing matches “xyz”").assertIsDisplayed()
        compose.onNodeWithText("Create “xyz”").performClick()

        assertThat(created).containsExactly("xyz")
    }

    @Test
    fun aNewQueryStartsAtItsBestMatchWhenTheOldFirstRowMovesDown() {
        val previous = (1..30).map { "Previous $it" }.toTypedArray()
        state = searched("p", *previous)
        show(query = "p")
        compose.onNodeWithText("Previous 1").assertIsDisplayed()

        state = searched("pr", *(1..10).map { "Better $it" }.toTypedArray(), *previous)

        compose.onNodeWithText("Better 1").assertIsDisplayed()
    }

    private fun searched(query: String, vararg names: String) = state.copy(
        filter = ExerciseFilter(query = query),
        sections = listOf(section(ExerciseSectionKind.RESULTS, *names)),
    )

    private fun section(kind: ExerciseSectionKind, vararg names: String) =
        ExerciseSectionUiModel(kind, names.map(::row))

    private fun show(query: String) {
        val field = TextFieldState(query)
        val actions = ExerciseListActions(
            onMuscleChange = {},
            onEquipmentChange = {},
            onOpenExercise = { opened += it.value },
            onCreateExercise = { created += it },
        )
        compose.setContent { LiftLogTheme { ExercisesScreen(state = state, query = field, actions = actions) } }
    }
}
