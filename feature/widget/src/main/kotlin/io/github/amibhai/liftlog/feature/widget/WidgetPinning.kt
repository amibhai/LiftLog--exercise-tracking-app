package io.github.amibhai.liftlog.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** Puts the widget on the home screen. The launcher asks first, and not every launcher can do it. */
object WidgetPinning {

    fun isSupported(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun request(context: Context) {
        AppWidgetManager.getInstance(context).requestPinAppWidget(widgetProvider(context), null, null)
    }
}

internal fun widgetProvider(context: Context) = ComponentName(context, LiftLogWidgetReceiver::class.java)
