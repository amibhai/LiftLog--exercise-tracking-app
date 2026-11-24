package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.ui.theme.LiftLogTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** Robolectric Compose tests for the logging components: behaviour and TalkBack labels. */
@RunWith(AndroidJUnit4::class)
class ComponentUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val row = SetRowUiModel(
        id = "set-2",
        type = SetType.WORKING,
        workingSetNumber = 2,
        previous = "80 × 8",
        fields = listOf(
            SetFieldUiModel(SetField.WEIGHT, text = "82.5", unit = "kg", isGhost = true),
            SetFieldUiModel(SetField.REPS, text = "8", unit = null, isGhost = true),
        ),
        suggestion = "+2.5 kg",
        isCompleted = false,
    )

    @Test
    fun switchRowTogglesAsAWholeAndReadsAsASwitch() {
        var checked by mutableStateOf(true)
        compose.setContent {
            LiftLogTheme { SwitchRow(title = "Dynamic color", checked = checked, onCheckedChange = { checked = it }) }
        }

        compose.onNodeWithText("Dynamic color")
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()

        compose.onNodeWithText("Dynamic color").assertIsOff()
    }

    @Test
    fun setRowCompletesWithOneTapAndAnnouncesItsState() {
        var current by mutableStateOf(row)
        val actions = mutableListOf<SetRowAction>()
        compose.setContent {
            LiftLogTheme {
                SetRow(
                    model = current,
                    onAction = { action ->
                        actions += action
                        if (action is SetRowAction.ToggleComplete) current = current.copy(isCompleted = true)
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("Complete set").assertIsOff().performClick()

        assertThat(actions).containsExactly(SetRowAction.ToggleComplete("set-2"))
        compose.onNodeWithContentDescription("Complete set").assertIsOn()
    }

    @Test
    fun setRowLabelsGhostValuesAndHistoryForTalkBack() {
        compose.setContent { LiftLogTheme { SetRow(model = row, onAction = {}) } }

        compose.onNodeWithContentDescription("Set 2").assertExists()
        compose.onNodeWithContentDescription("Previous: 80 × 8").assertExists()
        compose.onNodeWithContentDescription("Weight 82.5 kg, suggested").assertExists()
    }

    @Test
    fun tappingAFieldAsksToEditIt() {
        val actions = mutableListOf<SetRowAction>()
        compose.setContent { LiftLogTheme { SetRow(model = row, onAction = { actions += it }) } }

        compose.onNodeWithContentDescription("Reps 8, suggested").performClick()

        assertThat(actions).containsExactly(SetRowAction.EditField("set-2", SetField.REPS))
    }

    @Test
    fun numberPadTypesStepsAndReusesThePreviousValue() {
        var input by mutableStateOf(
            NumberInputState.start(BigDecimal("80"), maxDecimals = 2, maxValue = BigDecimal("999")),
        )
        compose.setContent {
            LiftLogTheme {
                NumberPad(
                    state = padState(input.text),
                    onKey = { input = input.reduce(it, step = BigDecimal("2.5"), previous = BigDecimal("75")) },
                )
            }
        }

        compose.onNodeWithText("9").performClick()
        compose.onNodeWithText("5").performClick()
        compose.onNodeWithContentDescription("Increase by 2.5").performClick()
        assertThat(input.text).isEqualTo("97.5")

        compose.onNodeWithContentDescription("Same as previous").performClick()
        assertThat(input.text).isEqualTo("75")
    }

    @Test
    fun numberPadDisablesTheDecimalKeyForWholeNumbers() {
        compose.setContent {
            LiftLogTheme { NumberPad(state = padState("8").copy(allowsDecimal = false), onKey = {}) }
        }

        compose.onNodeWithContentDescription("Decimal point").assertIsNotEnabled()
    }

    @Test
    fun restTimerShowsTimeLeftFromTheEndTimestamp() {
        compose.mainClock.autoAdvance = false
        val adjustments = mutableListOf<Int>()
        var skipped = false
        compose.setContent {
            LiftLogTheme {
                RestTimerBar(
                    state = RestTimerUiState(endsAtMillis = NOW + 90_000, totalSeconds = 120),
                    onAdjust = { adjustments += it },
                    onSkip = { skipped = true },
                    clock = { NOW },
                )
            }
        }

        compose.onNodeWithContentDescription("Rest, 1:30 remaining").assertExists()
        compose.onNodeWithContentDescription("Add 15 seconds").performClick()
        compose.onNodeWithContentDescription("Subtract 15 seconds").performClick()
        compose.onNodeWithText("Skip").performClick()

        assertThat(adjustments).containsExactly(15, -15).inOrder()
        assertThat(skipped).isTrue()
    }

    @Test
    fun restTimerKeepsEveryButtonReachableAtDoubleFontSizeOnANarrowPhone() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                LiftLogTheme {
                    RestTimerBar(
                        state = RestTimerUiState(endsAtMillis = NOW + 90_000, totalSeconds = 120),
                        onAdjust = {},
                        onSkip = {},
                        modifier = Modifier.width(320.dp),
                        clock = { NOW },
                    )
                }
            }
        }

        for (button in listOf("Subtract 15 seconds", "Add 15 seconds")) {
            compose.onNodeWithContentDescription(button).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithText("Skip").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun restTimerReadsZeroWhenTheRestIsOver() {
        compose.setContent {
            LiftLogTheme {
                RestTimerBar(
                    state = RestTimerUiState(endsAtMillis = NOW - 1, totalSeconds = 120),
                    onAdjust = {},
                    onSkip = {},
                    clock = { NOW },
                )
            }
        }

        compose.onNodeWithContentDescription("Rest over").assertExists()
    }

    private fun padState(text: String) = NumberPadUiState(
        fieldLabel = "Weight (kg)",
        valueText = text,
        stepLabel = "2.5",
        allowsDecimal = true,
        canUsePrevious = true,
        isLastField = false,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
