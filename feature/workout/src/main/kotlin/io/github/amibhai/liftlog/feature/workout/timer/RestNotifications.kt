package io.github.amibhai.liftlog.feature.workout.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.ui.format.workoutTitleRes
import io.github.amibhai.liftlog.feature.workout.R
import io.github.amibhai.liftlog.core.ui.R as UiR

/** The rest timer's notifications: the countdown while the app is away, and the end of the rest. */
internal class RestNotifications(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /**
     * Sound and vibration belong to a channel and cannot change once it exists, so each combination of
     * the timer settings has its own channel. Android 17 mutes sounds that a background app plays itself,
     * so the end of rest must come with a notification's sound to be heard.
     */
    fun createChannels() {
        manager.createNotificationChannelGroup(
            NotificationChannelGroupCompat.Builder(
                GROUP,
            ).setName(context.getString(R.string.rest_channel_group)).build(),
        )
        manager.createNotificationChannelsCompat(
            listOf(
                channel(COUNTDOWN, R.string.rest_channel_countdown, NotificationManagerCompat.IMPORTANCE_LOW),
                channel(OVER, R.string.rest_channel_over, NotificationManagerCompat.IMPORTANCE_HIGH, true, true),
                channel(
                    OVER_VIBRATE,
                    R.string.rest_channel_over_vibrate,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                    vibrates = true,
                ),
                channel(
                    OVER_SOUND,
                    R.string.rest_channel_over_sound,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                    sounds = true,
                ),
            ),
        )
    }

    private fun channel(id: String, name: Int, importance: Int, sounds: Boolean = false, vibrates: Boolean = false) =
        NotificationChannelCompat.Builder(id, importance)
            .setName(context.getString(name))
            .setGroup(GROUP)
            .setShowBadge(false)
            .setSound(if (sounds) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null, AUDIO)
            .setVibrationEnabled(vibrates)
            .setVibrationPattern(if (vibrates) VIBRATION else null)
            .build()

    // Every post goes through mayPost(): notifications need POST_NOTIFICATIONS from Android 13.
    @SuppressLint("MissingPermission")
    fun showCountdown(rest: ActiveRest, now: Long) {
        if (!mayPost()) return
        val title = rest.title.name ?: context.getString(rest.title.partOfDay.workoutTitleRes())
        val notification = NotificationCompat.Builder(context, COUNTDOWN)
            .setSmallIcon(UiR.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.rest_countdown_title))
            .setContentText(title)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(rest.endsAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp())
            .addAction(0, context.getString(R.string.rest_add_15), action(RestTimerReceiver.ACTION_ADD))
            .addAction(0, context.getString(R.string.rest_skip), action(RestTimerReceiver.ACTION_SKIP))
            // A status bar chip with the time left, on Android 16 and later.
            .setRequestPromotedOngoing(true)
            .setTimeoutAfter((rest.endsAt - now).coerceAtLeast(1))
            .build()
        manager.notify(COUNTDOWN_ID, notification)
    }

    @SuppressLint("MissingPermission") // See showCountdown.
    fun showRestOver(settings: UserSettings) {
        if (!mayPost()) return
        val channel = when {
            settings.restTimerSound && settings.restTimerVibration -> OVER
            settings.restTimerSound -> OVER_SOUND
            else -> OVER_VIBRATE
        }
        val notification: Notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(UiR.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.rest_over_title))
            .setContentText(context.getString(R.string.rest_over_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .setSilent(!settings.restTimerSound && !settings.restTimerVibration)
            .setTimeoutAfter(REST_OVER_TIMEOUT_MS)
            .build()
        manager.notify(REST_OVER_ID, notification)
    }

    fun cancelCountdown() = manager.cancel(COUNTDOWN_ID)

    fun cancelRestOver() = manager.cancel(REST_OVER_ID)

    private fun mayPost(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    /** Brings the app back as it was left, workout screen or banner. */
    private fun openApp(): PendingIntent? = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.let { PendingIntent.getActivity(context, REQUEST_OPEN, it, PendingIntent.FLAG_IMMUTABLE) }

    private fun action(name: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        name.hashCode(),
        Intent(context, RestTimerReceiver::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val GROUP = "rest_timer"
        const val COUNTDOWN = "rest_countdown"
        const val OVER = "rest_over"
        const val OVER_VIBRATE = "rest_over_vibrate"
        const val OVER_SOUND = "rest_over_sound"
        private const val COUNTDOWN_ID = 1
        private const val REST_OVER_ID = 2
        private const val REQUEST_OPEN = 0
        private const val REST_OVER_TIMEOUT_MS = 60_000L

        val VIBRATION = longArrayOf(0, 400, 200, 400, 200, 600)

        private val AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
