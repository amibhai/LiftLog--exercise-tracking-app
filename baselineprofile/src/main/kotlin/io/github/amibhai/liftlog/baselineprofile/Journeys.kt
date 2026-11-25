package io.github.amibhai.liftlog.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
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
private const val TRIES = 5
private const val SWIPE_STEPS = 10
private const val SEARCH_SWIPES = 4

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
    // A workout asks for this permission, and its dialog would stop the journey.
    device.executeShellCommand("pm grant $targetAppId android.permission.POST_NOTIFICATIONS")
    // A long run outlasts the screen timeout, and nothing can be found on a dark screen.
    device.wakeUp()
    pressHome()
    startActivityAndWait()
    if (device.wait(Until.hasObject(By.text("Skip")), SHORT_TIMEOUT_MS)) tap(By.text("Skip"))
    waitFor(By.text("Start empty workout"))
}

/** Starts a workout, adds exercises, logs sets, scrolls, and discards it again. */
internal fun MacrobenchmarkScope.logWorkout() {
    // A journey that broke off can leave a workout in progress, and then no new one starts.
    if (device.hasObject(By.text("Resume"))) {
        tap(By.text("Resume"))
        discardWorkout()
    }
    startWorkout(Lifts.take(FEW))
    // The first set is typed in; the ones after it come filled in with its numbers, as in the gym.
    tap(By.desc("Weight"))
    listOf("6", "0").forEach { tap(By.text(it)) }
    tap(By.desc("Next field"))
    tap(By.text("8"))
    tap(By.desc("Hide keypad"))
    repeat(SETS) {
        // Completed sets keep their button, which would undo them. The label belongs to its icon.
        tap(By.checkable(true).checked(false).hasDescendant(By.desc("Complete set")))
    }
    scroll()
    discardWorkout()
}

private fun MacrobenchmarkScope.discardWorkout() {
    tap(By.desc("More options"))
    tap(By.text("Discard workout"))
    tap(By.text("Discard"))
    waitFor(By.text("Start empty workout"))
}

/** A workout with ten exercises, on screen. It stays in progress, so it is only made once. */
internal fun MacrobenchmarkScope.openLongWorkout() {
    startApp()
    if (device.hasObject(By.text("Resume"))) tap(By.text("Resume")) else startWorkout(Lifts)
    waitFor(By.desc("Complete set"))
}

/** Every tab with a bit of scrolling, and the page of an exercise. */
internal fun MacrobenchmarkScope.browse() {
    scroll()
    for (tab in listOf("History", "Progress")) {
        tap(By.text(tab))
        scroll()
    }
    tap(By.text("Exercises"))
    // The popular lifts come first, before any scrolling.
    tap(By.text(Lifts.first()))
    tap(By.text("Progress"))
    device.pressBack()
    device.waitForIdle()
    scroll()
}

/** Down and back up the list on screen, if there is one. */
internal fun MacrobenchmarkScope.scroll() {
    repeat(FLINGS) { fling(Direction.DOWN) }
    fling(Direction.UP)
}

/** A quick swipe by screen coordinates, which cannot go stale. */
private fun MacrobenchmarkScope.fling(direction: Direction) {
    if (!device.wait(Until.hasObject(By.scrollable(true)), SHORT_TIMEOUT_MS)) return
    val area = retrying { waitFor(By.scrollable(true)).visibleBounds }
    // Away from the edges, where the gestures of the system are.
    val top = area.top + area.height() / EDGE
    val bottom = area.bottom - area.height() / EDGE
    if (direction == Direction.DOWN) {
        device.swipe(area.centerX(), bottom, area.centerX(), top, SWIPE_STEPS)
    } else {
        device.swipe(area.centerX(), top, area.centerX(), bottom, SWIPE_STEPS)
    }
    device.waitForIdle()
}

private fun MacrobenchmarkScope.startWorkout(lifts: List<String>) {
    tap(By.text("Start empty workout"))
    tap(By.text("Add exercises"))
    for (lift in lifts) {
        repeat(SEARCH_SWIPES) {
            if (!device.wait(Until.hasObject(By.text(lift)), SHORT_TIMEOUT_MS)) fling(Direction.DOWN)
        }
        tap(By.text(lift))
    }
    tap(By.textStartsWith("Add ("))
}

private fun MacrobenchmarkScope.waitFor(selector: BySelector): UiObject2 =
    checkNotNull(device.wait(Until.findObject(selector), TIMEOUT_MS)) { "Not on screen: $selector" }

/** Taps where the node is when it is found. */
private fun MacrobenchmarkScope.tap(selector: BySelector) {
    val center = retrying { waitFor(selector).visibleCenter }
    device.click(center.x, center.y)
    device.waitForIdle()
}

/**
 * Animations and the ticking rest timer make Compose replace nodes all the time, so a node can be gone by the time
 * it is read; then it is found again once the screen settled.
 */
private fun <T> MacrobenchmarkScope.retrying(read: () -> T): T {
    repeat(TRIES - 1) {
        try {
            return read()
        } catch (_: StaleObjectException) {
            device.waitForIdle()
        }
    }
    return read()
}
