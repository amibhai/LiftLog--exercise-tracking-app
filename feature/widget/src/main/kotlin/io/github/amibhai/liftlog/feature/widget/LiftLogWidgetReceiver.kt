package io.github.amibhai.liftlog.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** What the system knows the widget by; its name must never change, or placed widgets disappear. */
class LiftLogWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = LiftLogWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        widgetEntryPoint(context).widgetUpdater().onPlacedChanged()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        widgetEntryPoint(context).widgetUpdater().onPlacedChanged()
    }
}
