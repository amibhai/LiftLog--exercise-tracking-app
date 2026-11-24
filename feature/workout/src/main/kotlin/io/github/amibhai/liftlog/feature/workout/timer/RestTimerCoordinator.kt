package io.github.amibhai.liftlog.feature.workout.timer

import android.app.Application
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.time.partOfDay
import io.github.amibhai.liftlog.core.domain.workout.adjustedBy
import io.github.amibhai.liftlog.core.ui.format.WorkoutTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** A rest in progress, as the notifications show it. */
internal data class ActiveRest(val workoutId: WorkoutId, val title: WorkoutTitle, val endsAt: Long)

/**
 * Takes the rest timer outside the app (SPEC §6). The stored workout holds the timer as an absolute end
 * time; from it this schedules an alarm for the end of the rest and, while the app is not on screen,
 * shows the countdown in an ongoing notification. No foreground service is needed: the notification's
 * chronometer counts down by itself and the alarm wakes the app at the end.
 */
@Singleton
class RestTimerCoordinator internal constructor(
    private val context: Context,
    private val workouts: WorkoutRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        workouts: WorkoutRepository,
        settings: SettingsRepository,
        clock: Clock,
    ) : this(context, workouts, settings, clock, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    internal val visibility = AppVisibility()
    private val notifications = RestNotifications(context)
    private val alarms = RestAlarms(context)
    private val alert = RestAlert(context)
    private var isStarted = false

    /** Called once, from `Application.onCreate`. */
    fun start(application: Application) {
        if (isStarted) return
        isStarted = true
        application.registerActivityLifecycleCallbacks(visibility)
        notifications.createChannels()
        scope.launch {
            combine(activeRest(), visibleWithoutFlicker()) { rest, visible -> rest to visible }
                .collect { (rest, visible) -> show(rest, visible) }
        }
    }

    /** Handles the alarm and the notification's buttons; [onDone] ends the broadcast. */
    internal fun handle(intent: Intent, onDone: () -> Unit) {
        scope.launch {
            try {
                when (intent.action) {
                    RestTimerReceiver.ACTION_REST_OVER -> intent.getStringExtra(RestTimerReceiver.EXTRA_WORKOUT)?.let {
                        onRestOver(WorkoutId(it), intent.getLongExtra(RestTimerReceiver.EXTRA_ENDS_AT, 0))
                    }

                    RestTimerReceiver.ACTION_ADD -> adjust(ADD_SECONDS)

                    RestTimerReceiver.ACTION_SKIP -> adjust(null)
                }
            } finally {
                onDone()
            }
        }
    }

    /** The end of a rest. Does nothing when that rest was changed or skipped since, or ended long ago. */
    internal suspend fun onRestOver(workoutId: WorkoutId, endsAt: Long) {
        val workout = workouts.observeActive().first()
        val isLate = clock.millis() - endsAt > LATE_ALERT_MS
        if (workout?.id != workoutId || workout.restTimer?.endsAt != endsAt || isLate) return
        val settings = settings.settings.first()
        notifications.cancelCountdown()
        if (visibility.isVisible.value) alert.play(settings) else notifications.showRestOver(settings)
        // The timer bar says "Rest over" for a moment, then goes.
        scope.launch {
            delay(REST_OVER_SHOWN_MS)
            workouts.edit(workoutId) { if (it.restTimer?.endsAt == endsAt) it.copy(restTimer = null) else it }
        }
    }

    /** +15 s, or skip when [seconds] is null. */
    private suspend fun adjust(seconds: Int?) {
        val workout = workouts.observeActive().first() ?: return
        val now = clock.millis()
        workouts.edit(workout.id) { stored ->
            stored.copy(restTimer = seconds?.let { stored.restTimer?.adjustedBy(it, now) })
        }
    }

    private fun show(rest: ActiveRest?, isVisible: Boolean) {
        val now = clock.millis()
        when {
            rest == null -> {
                alarms.cancel()
                notifications.cancelCountdown()
            }

            // Its alarm is due, or has come already.
            rest.endsAt <= now -> notifications.cancelCountdown()

            else -> {
                alarms.schedule(rest.workoutId, rest.endsAt)
                notifications.cancelRestOver()
                if (isVisible) notifications.cancelCountdown() else notifications.showCountdown(rest, now)
            }
        }
    }

    private fun activeRest(): Flow<ActiveRest?> = workouts.observeActive()
        .map { workout -> workout?.restTimer?.let { ActiveRest(workout.id, workout.title(), it.endsAt) } }
        .distinctUntilChanged()

    /** Rotating the phone stops and starts the activity; that is no reason to flash a notification. */
    @OptIn(FlowPreview::class)
    private fun visibleWithoutFlicker(): Flow<Boolean> =
        visibility.isVisible.debounce { visible -> if (visible) 0 else HIDDEN_DEBOUNCE_MS }

    private fun Workout.title() = WorkoutTitle(name, partOfDay(startedAt, ZoneId.systemDefault()))

    internal companion object {
        const val ADD_SECONDS = 15
        const val REST_OVER_SHOWN_MS = 5_000L
        const val LATE_ALERT_MS = 120_000L
        const val HIDDEN_DEBOUNCE_MS = 500L
    }
}
