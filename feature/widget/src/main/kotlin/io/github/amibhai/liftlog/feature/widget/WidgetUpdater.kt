package io.github.amibhai.liftlog.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WidgetUpdater"

/**
 * Draws the widgets again when something they show has changed, or the font size, which decides how much fits.
 * While the app is not running, the system asks for it a few times a day, which is when a streak can end.
 */
@Singleton
class WidgetUpdater internal constructor(
    private val states: WidgetStates,
    private val scope: CoroutineScope,
    private val isPlaced: () -> Boolean,
    private val update: suspend () -> Unit,
    private val publishPreview: suspend () -> Unit,
    private val fontScale: Flow<Float>,
) {
    @Inject
    constructor(@ApplicationContext context: Context, states: WidgetStates) : this(
        states = states,
        // A widget that cannot be drawn must not end the app in the middle of a workout.
        scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default +
                CoroutineExceptionHandler { _, error -> Log.w(TAG, "The widget was not updated", error) },
        ),
        isPlaced = { placedWidgets(context).isNotEmpty() },
        update = { LiftLogWidget().updateAll(context) },
        publishPreview = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) publishPreviewOnce(context)
        },
        fontScale = fontScaleOf(context),
    )

    private val hasWidgets = MutableStateFlow(false)

    /** Called once, from `Application.onCreate`. */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start() {
        scope.launch {
            onPlacedChanged()
            hasWidgets.flatMapLatest { placed ->
                if (placed) combine(states.observe(), fontScale) { state, scale -> state to scale } else emptyFlow()
            }
                .debounce(UPDATE_DELAY_MS)
                .collect { update() }
        }
        scope.launch {
            delay(PREVIEW_DELAY_MS)
            publishPreview()
        }
    }

    /** The first widget was placed, or the last one removed. */
    internal fun onPlacedChanged() {
        hasWidgets.value = isPlaced()
    }

    internal companion object {
        const val UPDATE_DELAY_MS = 500L
        const val PREVIEW_DELAY_MS = 10_000L
    }
}

private fun placedWidgets(context: Context): IntArray =
    AppWidgetManager.getInstance(context).getAppWidgetIds(widgetProvider(context))

/** The font size of the system. Apps are not told about changes while they are not running. */
internal fun fontScaleOf(context: Context): Flow<Float> = callbackFlow {
    val callbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            trySend(newConfig.fontScale)
        }

        @Deprecated("The system no longer calls it")
        override fun onLowMemory() = Unit
    }
    send(context.resources.configuration.fontScale)
    context.registerComponentCallbacks(callbacks)
    awaitClose { context.unregisterComponentCallbacks(callbacks) }
}.distinctUntilChanged()

/** What the widget picker shows; it stays until the app is updated. */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private suspend fun publishPreviewOnce(context: Context) {
    val info = AppWidgetManager.getInstance(context)
        .getInstalledProvidersForPackage(context.packageName, null)
        .firstOrNull { it.provider == widgetProvider(context) }
    if (info?.generatedPreviewCategories != 0) return
    val result = GlanceAppWidgetManager(context).setWidgetPreviews(LiftLogWidgetReceiver::class)
    if (result != GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) Log.w(TAG, "No preview for now")
}
