package io.github.amibhai.liftlog.core.ui.launch

import android.content.Context
import android.content.Intent

/**
 * What a shortcut or the widget asks of the app (SPEC §11). The intent actions are also in
 * `app/src/main/res/xml/shortcuts.xml`. With a workout in progress all of them open that one.
 */
enum class LaunchAction(val intentAction: String) {
    START_EMPTY("io.github.amibhai.liftlog.action.START_EMPTY"),

    /** The next day of the followed program, or an empty workout. */
    START_NEXT("io.github.amibhai.liftlog.action.START_NEXT"),

    REPEAT_LAST("io.github.amibhai.liftlog.action.REPEAT_LAST"),
    ;

    /** Opens the app to do it. */
    fun intent(context: Context): Intent = openAppIntent(context).setAction(intentAction)

    companion object {
        fun of(intent: Intent?): LaunchAction? = entries.firstOrNull { it.intentAction == intent?.action }
    }
}

/** Brings the app to the front as it was left, or starts it. Only tests have no launcher activity. */
fun openAppIntent(context: Context): Intent {
    val launcher = context.packageManager.getLaunchIntentForPackage(context.packageName)
    return (launcher ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName))
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
}
