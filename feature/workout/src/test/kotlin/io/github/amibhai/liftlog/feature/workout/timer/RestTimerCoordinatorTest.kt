package io.github.amibhai.liftlog.feature.workout.timer

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.RestTimer
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import io.github.amibhai.liftlog.feature.workout.NOW
import io.github.amibhai.liftlog.feature.workout.bench
import io.github.amibhai.liftlog.feature.workout.exercise
import io.github.amibhai.liftlog.feature.workout.set
import io.github.amibhai.liftlog.feature.workout.testClock
import io.github.amibhai.liftlog.feature.workout.values
import io.github.amibhai.liftlog.feature.workout.workout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RestTimerCoordinatorTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val endsAt = NOW + 90_000
    private val id = WorkoutId("w")
    private val workouts = FakeWorkoutRepository(
        workout(exercise("e", bench, set("1", target = values(82.5, 8))))
            .copy(restTimer = RestTimer(endsAt, totalSeconds = 90)),
    )
    private val posted get() = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications

    @Before
    fun allowNotifications() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun awayFromTheAppTheRestCountsDownInANotification() = runTest {
        start()
        settle()

        val countdown = posted.single()
        assertThat(countdown.channelId).isEqualTo(RestNotifications.COUNTDOWN)
        assertThat(countdown.`when`).isEqualTo(endsAt)
        assertThat(countdown.extras.getBoolean(NotificationCompat.EXTRA_CHRONOMETER_COUNT_DOWN)).isTrue()
        assertThat(NotificationCompat.isRequestPromotedOngoing(countdown)).isTrue()
        assertThat(countdown.extras.getString(NotificationCompat.EXTRA_TEXT))
            .isEqualTo("Next: Bench Press, 82.5 kg × 8")
        assertThat(countdown.actions.map { it.title.toString() }).containsExactly("+15s", "Skip").inOrder()
        val alarm = shadowOf(context.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm()
        assertThat(alarm?.triggerAtMs).isEqualTo(endsAt)
    }

    @Test
    fun aCountdownThatWasSwipedAwayStaysAway() = runTest {
        val coordinator = start()
        settle()
        val swiped = Intent(RestTimerReceiver.ACTION_DISMISSED).putExtra(RestTimerReceiver.EXTRA_ENDS_AT, endsAt)
        coordinator.handle(swiped) {}
        context.getSystemService(NotificationManager::class.java).cancelAll()

        coordinator.visibility.started()
        settle()
        coordinator.visibility.stopped()
        settle()
        assertThat(posted).isEmpty()

        workouts.edit(id) { it.copy(restTimer = RestTimer(endsAt + 15_000, totalSeconds = 105)) }
        settle()
        assertThat(posted).hasSize(1)
    }

    @Test
    fun onScreenTheTimerStaysInTheApp() = runTest {
        val coordinator = start()
        coordinator.visibility.started()
        settle()

        assertThat(posted).isEmpty()
    }

    @Test
    fun theEndOfTheRestAlertsAndThenClearsTheTimer() = runTest {
        val coordinator = start()
        settle()

        coordinator.onRestOver(id, endsAt)
        runCurrent()
        assertThat(posted.map { it.channelId }).containsExactly(RestNotifications.OVER)

        advanceTimeBy(RestTimerCoordinator.REST_OVER_SHOWN_MS + 1)
        assertThat(workouts[id]!!.restTimer).isNull()
    }

    @Test
    fun aRestThatWasChangedSinceDoesNotAlert() = runTest {
        val coordinator = start()
        settle()

        coordinator.onRestOver(id, endsAt - 15_000)
        settle()

        assertThat(posted.map { it.channelId }).containsExactly(RestNotifications.COUNTDOWN)
        assertThat(workouts[id]!!.restTimer?.endsAt).isEqualTo(endsAt)
    }

    /**
     * Runs what is due within a second. `advanceUntilIdle` would not do: it stops once no foreground work
     * is left, and the coordinator runs in the background scope.
     */
    private fun TestScope.settle() {
        advanceTimeBy(SETTLE_MS)
        runCurrent()
    }

    private fun TestScope.start() =
        RestTimerCoordinator(context, workouts, FakeSettingsRepository(), testClock, backgroundScope).also {
            it.start(context)
        }

    private companion object {
        const val SETTLE_MS = 1_000L
    }
}
