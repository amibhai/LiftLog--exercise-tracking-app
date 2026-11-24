package io.github.amibhai.liftlog.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.ui.theme.LiftLogTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var page by mutableStateOf(OnboardingPage.UNIT)
    private val actions = OnboardingActions(
        onUnitChange = { calls += it.name },
        onBarChange = { calls += "bar $it" },
        onTogglePlate = { calls += "plate $it" },
        onNext = { calls += "next" },
        onBack = { calls += "back" },
        onChooseProgram = { calls += "program" },
        onSkip = { calls += "skip" },
    )

    @Test
    fun theUnitIsChosenFirst() {
        show()

        composeRule.onNodeWithText("1 of 3").assertIsDisplayed()
        composeRule.onNodeWithText("kg").assertIsSelected()
        composeRule.onNodeWithText("lb").performClick()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Skip").performClick()

        assertThat(calls).containsExactly("LB", "next", "skip").inOrder()
    }

    @Test
    fun theBarAndThePlatesAreChosenNext() {
        page = OnboardingPage.PLATES
        show()

        // There are bars and plates of 15 kg; the bars come first.
        composeRule.onAllNodesWithText("15 kg")[0].performClick()
        composeRule.onNodeWithText("0.5 kg").performScrollTo().performClick()

        assertThat(calls).containsExactly("bar 15.0", "plate 0.5").inOrder()
    }

    @Test
    fun aProgramIsOptional() {
        page = OnboardingPage.PROGRAM
        show()

        composeRule.onNodeWithText("Choose a program").performClick()
        composeRule.onNodeWithText("Start without one").performClick()
        composeRule.onNodeWithText("Skip").assertDoesNotExist()

        assertThat(calls).containsExactly("program", "skip").inOrder()
    }

    private fun show() = composeRule.setContent {
        LiftLogTheme { OnboardingScreen(page, UserSettings(), actions) }
    }
}
