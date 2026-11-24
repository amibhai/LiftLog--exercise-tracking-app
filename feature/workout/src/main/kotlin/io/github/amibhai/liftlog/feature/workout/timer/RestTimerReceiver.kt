package io.github.amibhai.liftlog.feature.workout.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** The end-of-rest alarm, and the +15 s and Skip buttons of the countdown notification. */
class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val coordinator = EntryPointAccessors
            .fromApplication(context.applicationContext, RestTimerEntryPoint::class.java)
            .restTimer()
        val pending = goAsync()
        coordinator.handle(intent) { pending.finish() }
    }

    internal companion object {
        const val ACTION_REST_OVER = "io.github.amibhai.liftlog.action.REST_OVER"
        const val ACTION_ADD = "io.github.amibhai.liftlog.action.REST_ADD"
        const val ACTION_SKIP = "io.github.amibhai.liftlog.action.REST_SKIP"
        const val EXTRA_WORKOUT = "workout"
        const val EXTRA_ENDS_AT = "ends_at"
    }
}

/** Receivers are created by the system, so the coordinator is looked up rather than injected. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RestTimerEntryPoint {
    fun restTimer(): RestTimerCoordinator
}
