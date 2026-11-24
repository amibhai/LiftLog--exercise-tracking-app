package io.github.amibhai.liftlog.feature.workout.timer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Whether an activity of the app is on screen; only then does the timer stay inside the app. */
internal class AppVisibility : Application.ActivityLifecycleCallbacks {

    private val started = MutableStateFlow(0)
    private val visible = MutableStateFlow(false)

    val isVisible: StateFlow<Boolean> = visible.asStateFlow()

    fun started() {
        started.update { it + 1 }
        visible.value = started.value > 0
    }

    fun stopped() {
        started.update { (it - 1).coerceAtLeast(0) }
        visible.value = started.value > 0
    }

    override fun onActivityStarted(activity: Activity) = started()

    override fun onActivityStopped(activity: Activity) = stopped()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
