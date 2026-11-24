package io.github.amibhai.liftlog.feature.workout.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import io.github.amibhai.liftlog.core.domain.model.UserSettings

/** The end of a rest while the app is on screen: no notification, just the vibration and sound. */
internal class RestAlert(private val context: Context) {

    fun play(settings: UserSettings) {
        if (settings.restTimerVibration) {
            context.getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createWaveform(RestNotifications.VIBRATION, NO_REPEAT))
        }
        if (settings.restTimerSound) {
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                ?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ?.play()
        }
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}
