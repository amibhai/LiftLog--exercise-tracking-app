package io.github.amibhai.liftlog.feature.widget

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.testing.FakeProgramRepository
import io.github.amibhai.liftlog.core.testing.FakeProgressRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetUpdaterTest {

    private val workouts = FakeWorkoutRepository()
    private val states = WidgetStates(
        workouts,
        FakeProgramRepository(),
        FakeProgressRepository(),
        FakeSettingsRepository(),
        Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
    )
    private var isPlaced = true
    private var updates = 0
    private var previews = 0

    @Test
    fun widgetsAreDrawnAgainWhenSomethingChanges() = runTest {
        start()
        settle()
        assertThat(updates).isEqualTo(1)

        workouts.create(Workout(id = WorkoutId("w"), startedAt = 0, name = "Push"))
        settle()
        assertThat(updates).isEqualTo(2)
    }

    @Test
    fun withoutAWidgetNothingIsDone() = runTest {
        isPlaced = false
        val updater = start()
        settle()
        assertThat(updates).isEqualTo(0)

        isPlaced = true
        updater.onPlacedChanged()
        settle()
        assertThat(updates).isEqualTo(1)
    }

    @Test
    fun thePreviewWaitsUntilTheAppHasStarted() = runTest {
        start()
        settle()
        assertThat(previews).isEqualTo(0)

        advanceTimeBy(WidgetUpdater.PREVIEW_DELAY_MS)
        runCurrent()
        assertThat(previews).isEqualTo(1)
    }

    private fun TestScope.start() = WidgetUpdater(
        states = states,
        scope = backgroundScope,
        isPlaced = { isPlaced },
        update = { updates++ },
        publishPreview = { previews++ },
    ).also { it.start() }

    private fun TestScope.settle() {
        advanceTimeBy(WidgetUpdater.UPDATE_DELAY_MS + 1)
        runCurrent()
    }
}
