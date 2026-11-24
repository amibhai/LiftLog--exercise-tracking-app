package io.github.amibhai.liftlog.feature.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.amibhai.liftlog.core.domain.model.ThemeMode
import io.github.amibhai.liftlog.core.ui.format.workoutTitleRes
import io.github.amibhai.liftlog.core.ui.launch.LaunchAction
import io.github.amibhai.liftlog.core.ui.launch.openAppIntent
import io.github.amibhai.liftlog.core.ui.theme.liftLogColorScheme
import io.github.amibhai.liftlog.core.ui.R as UiR

/** The widget in the colors of the app. */
@Composable
internal fun ThemedWidget(state: WidgetState) {
    val context = LocalContext.current
    val day = liftLogColorScheme(context, darkTheme = state.themeMode == ThemeMode.DARK, state.useDynamicColor)
    val night = liftLogColorScheme(context, darkTheme = state.themeMode != ThemeMode.LIGHT, state.useDynamicColor)
    GlanceTheme(ColorProviders(light = day, dark = night)) { WidgetContent(state) }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val open = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .roundedBackground(GlanceTheme.colors.surface)
        .clickable(actionStartActivity(openAppIntent(context)))
    if (size.height >= WidgetSize.CARD.size.height) {
        Column(modifier = open.padding(16.dp)) {
            Lines(state, isTall = size.height >= WidgetSize.TALL_CARD.size.height)
            StartButton(state, GlanceModifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = open.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StartButton(state, GlanceModifier.defaultWeight())
            if (size.width >= WidgetSize.BAR.size.width && state.streakWeeks > 0) {
                Spacer(GlanceModifier.width(12.dp))
                Streak(state.streakWeeks)
            }
        }
    }
}

/** What is going on, above the button. */
@Composable
private fun ColumnScope.Lines(state: WidgetState, isTall: Boolean) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
        Line(state.title(context), GlanceTheme.colors.onSurface, isTitle = true)
        Line(state.streakText(context), GlanceTheme.colors.onSurfaceVariant)
        if (isTall) {
            val count = state.workoutsThisWeek
            val week = context.resources.getQuantityString(R.plurals.widget_workouts, count, count)
            Line(week, GlanceTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun Line(text: String, color: ColorProvider, isTitle: Boolean = false) {
    Text(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = if (isTitle) 16.sp else 14.sp,
            fontWeight = if (isTitle) FontWeight.Medium else FontWeight.Normal,
        ),
        maxLines = 1,
    )
}

/** Next to the button there is only room for the number. */
@Composable
private fun Streak(weeks: Int) {
    val context = LocalContext.current
    val label = context.resources.getQuantityString(R.plurals.widget_streak, weeks, weeks)
    Row(
        modifier = GlanceModifier.semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(UiR.drawable.ic_local_fire_department),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        )
        Spacer(GlanceModifier.width(4.dp))
        Line(weeks.toString(), GlanceTheme.colors.onSurface, isTitle = true)
    }
}

@Composable
private fun StartButton(state: WidgetState, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    FilledButton(
        text = state.buttonText(context),
        onClick = actionStartActivity(LaunchAction.START_NEXT.intent(context)),
        modifier = modifier.height(48.dp),
        icon = ImageProvider(UiR.drawable.ic_play_arrow),
        maxLines = 1,
    )
}

/** Widgets have had round corners since Android 12; before that the background is a shape. */
private fun GlanceModifier.roundedBackground(color: ColorProvider): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background(color).cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        background(ImageProvider(R.drawable.widget_background), colorFilter = ColorFilter.tint(color))
    }

private fun WidgetState.title(context: Context): String = when {
    active != null -> active.name ?: context.getString(active.partOfDay.workoutTitleRes())
    nextDay != null -> context.getString(R.string.widget_next, nextDay)
    else -> context.getString(R.string.widget_ready)
}

private fun WidgetState.streakText(context: Context): String = if (streakWeeks > 0) {
    context.resources.getQuantityString(R.plurals.widget_streak, streakWeeks, streakWeeks)
} else {
    context.getString(R.string.widget_no_streak)
}

private fun WidgetState.buttonText(context: Context): String = when {
    active != null -> context.getString(R.string.widget_resume)
    nextDay != null -> context.getString(R.string.widget_start_day, nextDay)
    else -> context.getString(R.string.widget_start)
}
