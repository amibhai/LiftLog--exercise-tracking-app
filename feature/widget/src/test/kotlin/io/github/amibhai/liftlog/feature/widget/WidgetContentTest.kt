package io.github.amibhai.liftlog.feature.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import io.github.amibhai.liftlog.core.domain.time.PartOfDay
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun withoutAWorkoutOneCanBeStarted() = showing(WidgetState()) {
        onNode(hasText("Start")).assertExists()
        onNode(hasContentDescriptionEqualTo("Start a workout")).assertExists()
        onNode(hasText("Ready to train")).assertExists()
        onNode(hasText("No streak yet")).assertExists()
    }

    @Test
    fun theWorkoutInProgressCanBeResumed() = showing(WidgetState(active = WorkoutTitle("Push", PartOfDay.EVENING))) {
        onNode(hasText("Resume")).assertExists()
        onNode(hasContentDescriptionEqualTo("Resume the workout")).assertExists()
        onNode(hasText("Push")).assertExists()
    }

    @Test
    fun theNextDayOfTheProgramCanBeStarted() =
        showing(WidgetState(nextDay = "Legs", streakWeeks = 3, workoutsThisWeek = 1)) {
            onNode(hasContentDescriptionEqualTo("Start Legs")).assertExists()
            onNode(hasText("Next up: Legs")).assertExists()
            onNode(hasText("3-week streak")).assertExists()
            onNode(hasText("1 workout this week")).assertExists()
        }

    @Test
    fun largeTextLeavesOutTheThirdLine() = showing(WidgetState(streakWeeks = 3, workoutsThisWeek = 1, fontScale = 2f)) {
        onNode(hasText("3-week streak")).assertExists()
        onNode(hasText("1 workout this week")).assertDoesNotExist()
    }

    @Test
    fun inOneRowTheStreakIsANumberNextToTheButton() = showing(WidgetState(streakWeeks = 3), WidgetSize.BAR.size) {
        onNode(hasText("Start")).assertExists()
        onNode(hasContentDescriptionEqualTo("3-week streak")).assertExists()
        onNode(hasText("Ready to train")).assertDoesNotExist()
    }

    @Test
    fun theSmallestOneIsOnlyTheButton() = showing(WidgetState(streakWeeks = 3), WidgetSize.BUTTON.size) {
        onNode(hasText("Start")).assertExists()
        onNode(hasContentDescriptionEqualTo("3-week streak")).assertDoesNotExist()
    }

    private fun showing(
        state: WidgetState,
        size: DpSize = WidgetSize.TALL_CARD.size,
        check: GlanceAppWidgetUnitTest.() -> Unit,
    ) = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(size)
        provideComposable { ThemedWidget(state) }
        check()
    }
}
