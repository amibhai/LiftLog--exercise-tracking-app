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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExercisesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var state by mutableStateOf(ExercisesUiState(totalExercises = 1_670))
    private val opened = mutableListOf<String>()

    @Test
    fun aBlankQueryShowsTheLibrarySize() {
        show(query = "")

        compose.onNodeWithText("Search 1,670 exercises").assertIsDisplayed()
    }

    @Test
    fun aQueryWithoutResultsSaysSo() {
        state = state.copy(searchedQuery = "xyz")
        show(query = "xyz")

        compose.onNodeWithText("Nothing matches “xyz”").assertIsDisplayed()
    }

    @Test
    fun tappingAResultOpensIt() {
        state = state.copy(searchedQuery = "bench", results = listOf(summary("Barbell Bench Press")))
        show(query = "bench")

        compose.onNodeWithText("Barbell Bench Press").performClick()

        assertThat(opened).containsExactly("Barbell Bench Press")
    }

    @Test
    fun aNewQueryStartsAtItsBestMatchWhenTheOldFirstRowMovesDown() {
        val previous = (1..30).map { summary("Previous $it") }
        state = state.copy(searchedQuery = "p", results = previous)
        show(query = "p")
        compose.onNodeWithText("Previous 1").assertIsDisplayed()

        state = state.copy(searchedQuery = "pr", results = (1..10).map { summary("Better $it") } + previous)

        compose.onNodeWithText("Better 1").assertIsDisplayed()
    }

    private fun show(query: String) {
        val queryState = TextFieldState(query)
        compose.setContent {
            LiftLogTheme {
                ExercisesScreen(state = state, queryState = queryState, onOpenExercise = { opened += it })
            }
        }
    }
}
