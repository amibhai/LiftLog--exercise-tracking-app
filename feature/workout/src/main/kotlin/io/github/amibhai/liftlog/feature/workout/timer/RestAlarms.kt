package io.github.amibhai.liftlog.feature.workout.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import io.github.amibhai.liftlog.core.domain.model.WorkoutId

/** One alarm at the end of the current rest; scheduling another replaces it. */
internal class RestAlarms(private val context: Context) {

    private val alarms: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(workoutId: WorkoutId, endsAt: Long) {
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_REST_OVER,
            restOver().putExtra(RestTimerReceiver.EXTRA_WORKOUT, workoutId.value)
                .putExtra(RestTimerReceiver.EXTRA_ENDS_AT, endsAt),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (AlarmManagerCompat.canScheduleExactAlarms(alarms)) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarms, AlarmManager.RTC_WAKEUP, endsAt, intent)
        } else {
            // Exact alarms switched off (possible on Android 12 only): the alert may come a little late.
            AlarmManagerCompat.setAndAllowWhileIdle(alarms, AlarmManager.RTC_WAKEUP, endsAt, intent)
        }
    }

    fun cancel() {
        PendingIntent.getBroadcast(
            context,
            REQUEST_REST_OVER,
            restOver(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )?.let(alarms::cancel)
    }

    private fun restOver() = Intent(
        context,
        RestTimerReceiver::class.java,
    ).setAction(RestTimerReceiver.ACTION_REST_OVER)

    private companion object {
        const val REQUEST_REST_OVER = 1
    }
}
