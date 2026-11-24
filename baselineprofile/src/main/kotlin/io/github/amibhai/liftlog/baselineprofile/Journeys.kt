package io.github.amibhai.liftlog.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** The app that Gradle installed for the test (`….benchmark`), never the one that is trained with. */
internal val targetAppId: String =
    checkNotNull(InstrumentationRegistry.getArguments().getString("androidx.benchmark.targetPackageName")) {
        "Run the journeys with Gradle: the baseline profile plugin says which app to drive"
    }

private const val TIMEOUT_MS = 10_000L
private const val SHORT_TIMEOUT_MS = 3_000L
private const val FLINGS = 2
private const val EDGE = 5
private const val SETS = 3
private const val FEW = 3

/** What the exercise picker suggests before there is any history. */
private val Lifts = listOf(
    "Barbell Bench Press",
    "Barbell Back Squat",
    "Barbell Deadlift",
    "Barbell Overhead Press",
    "Bent Over Barbell Row",
    "Pull-Up",
    "Wide-Grip Lat Pulldown",
    "Romanian Deadlift",
    "Incline Dumbbell Press",
    "Leg Press",
)

/** Cold start up to Home; the first-run pages are skipped. */
internal fun MacrobenchmarkScope.startApp() {
    pressHome()
    startActivityAndWait()
    device.wait(Until.findObject(By.text("Skip")), SHORT_TIMEOUT_MS)?.click()
    waitFor(By.text("Start empty workout"))
}

/** Starts a workout, adds exercises, logs sets, scrolls, and discards it again. */
internal fun MacrobenchmarkScope.logWorkout() {
    startWorkout(Lifts.take(FEW))
    repeat(SETS) {
        // An empty weight field: the sets that follow may come filled in already.
        device.findObject(By.desc("Weight"))?.let { field ->
            field.click()
            listOf("6", "0").forEach { tap(By.text(it)) }
            tap(By.desc("Next field"))
            tap(By.text("8"))
            tap(By.desc("Hide keypad"))
        }
        // Completed sets keep their button, which would undo them.
        tap(By.desc("Complete set").checked(false))
    }
    scroll()
    tap(By.desc("More options"))
    tap(By.text("Discard workout"))
    tap(By.text("Discard"))
    waitFor(By.text("Start empty workout"))
}

/** A workout with ten exercises, on screen. It stays in progress, so it is only made once. */
internal fun MacrobenchmarkScope.openLongWorkout() {
    startApp()
    val resume = device.findObject(By.text("Resume"))
    if (resume == null) startWorkout(Lifts) else resume.click()
    waitFor(By.desc("Complete set"))
}

/** Every tab with a bit of scrolling, and the page of an exercise. */
internal fun MacrobenchmarkScope.browse() {
    scroll()
    for (tab in listOf("History", "Progress", "Exercises")) {
        tap(By.text(tab))
        scroll()
    }
    tap(By.text(Lifts.first()))
    tap(By.text("Progress"))
    device.pressBack()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.scroll() {
    val list = device.wait(Until.findObject(By.scrollable(true)), SHORT_TIMEOUT_MS) ?: return
    // Away from the edges, where the gestures of the system are.
    list.setGestureMargin(device.displayWidth / EDGE)
    repeat(FLINGS) { list.fling(Direction.DOWN) }
    list.fling(Direction.UP)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.startWorkout(lifts: List<String>) {
    tap(By.text("Start empty workout"))
    tap(By.text("Add exercises"))
    for (lift in lifts) {
        val row = device.wait(Until.findObject(By.text(lift)), SHORT_TIMEOUT_MS)
            ?: waitFor(By.scrollable(true)).scrollUntil(Direction.DOWN, Until.findObject(By.text(lift)))
        checkNotNull(row) { "Not in the picker: $lift" }.click()
        device.waitForIdle()
    }
    tap(By.textStartsWith("Add ("))
}

private fun MacrobenchmarkScope.waitFor(selector: BySelector): UiObject2 =
    checkNotNull(device.wait(Until.findObject(selector), TIMEOUT_MS)) { "Not on screen: $selector" }

private fun MacrobenchmarkScope.tap(selector: BySelector) {
    waitFor(selector).click()
    device.waitForIdle()
}
