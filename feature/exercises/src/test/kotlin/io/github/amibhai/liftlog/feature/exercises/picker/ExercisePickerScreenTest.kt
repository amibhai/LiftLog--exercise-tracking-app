package io.github.amibhai.liftlog.feature.exercises.picker

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.ui.theme.LiftLogTheme
import io.github.amibhai.liftlog.feature.exercises.ExerciseListActions
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseListUiState
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseSectionKind
import io.github.amibhai.liftlog.feature.exercises.list.ExerciseSectionUiModel
import io.github.amibhai.liftlog.feature.exercises.row
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExercisePickerScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var state by mutableStateOf(
        ExercisePickerUiState(
            list = ExerciseListUiState(
                isLoading = false,
                sections = listOf(
                    ExerciseSectionUiModel(
                        ExerciseSectionKind.RECENT,
                        listOf(row("Bench"), row("Curl")).map { it.copy(lastSet = "80 kg × 8", lastDaysAgo = 3) },
                    ),
                ),
            ),
        ),
    )
    private val opened = mutableListOf<ExerciseId>()
    private var addedAsSuperset: Boolean? = null

    @Test
    fun tappingSelectsAndTheButtonCounts() {
        show()
        compose.onNodeWithText("Bench").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Add (1)").assertExists()
        compose.onNodeWithText("Add as superset").assertDoesNotExist()

        compose.onNodeWithText("Curl").performClick()
        compose.onNodeWithText("Add (2)").performClick()
        assertThat(addedAsSuperset).isFalse()

        compose.onNodeWithText("Add as superset").performClick()
        assertThat(addedAsSuperset).isTrue()
    }

    @Test
    fun rowsTellWhatWasDoneLast() {
        show()

        compose.onNodeWithText("Bench").assert(
            hasText("Last: 80 kg × 8 · 3 days ago", substring = true),
        )
    }

    @Test
    fun aLongPressOpensTheDetails() {
        show()

        compose.onNodeWithText("Bench").performTouchInput { longClick() }

        assertThat(opened).containsExactly(ExerciseId("Bench"))
    }

    private fun show() {
        val actions = ExercisePickerActions(
            list = ExerciseListActions({}, {}, onOpenExercise = { opened += it }, onCreateExercise = {}),
            onPick = { id ->
                state = state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id)
            },
            onAdd = { addedAsSuperset = it },
            onClose = {},
        )
        val query = TextFieldState()
        compose.setContent { LiftLogTheme { ExercisePickerScreen(state = state, query = query, actions = actions) } }
    }
}
