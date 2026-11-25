package io.github.amibhai.liftlog.feature.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** Starts or resumes a workout and shows the weekly streak (SPEC §11). */
internal class LiftLogWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(WidgetSize.entries.map { it.size }.toSet())

    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetSize.CARD.size))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A composition runs on for a while after an update, so it has to follow the font size itself.
        val states = combine(widgetEntryPoint(context).widgetStates().observe(), fontScaleOf(context)) { state, scale ->
            state.copy(fontScale = scale)
        }
        val first = states.first()
        provideContent {
            val state by states.collectAsState(first)
            ThemedWidget(state)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val sample = WidgetState(
            nextDay = context.getString(R.string.widget_preview_day),
            streakWeeks = SAMPLE_STREAK,
            workoutsThisWeek = SAMPLE_WORKOUTS,
            useDynamicColor = false,
        )
        provideContent { ThemedWidget(sample) }
    }

    private companion object {
        const val SAMPLE_STREAK = 8
        const val SAMPLE_WORKOUTS = 2
    }
}

/** Widgets are created by the system, so what they need is looked up rather than injected. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun widgetStates(): WidgetStates

    fun widgetUpdater(): WidgetUpdater
}

internal fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
