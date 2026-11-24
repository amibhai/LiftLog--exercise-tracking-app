package io.github.amibhai.liftlog.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes down the code that the app needs at its start and on the way through it, for the phone to compile
 * ahead of time. Run it with `./gradlew :app:generateBaselineProfile`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = targetAppId, includeInStartupProfile = true) { startApp() }

    @Test
    fun logging() = rule.collect(packageName = targetAppId, maxIterations = ITERATIONS) {
        startApp()
        logWorkout()
    }

    @Test
    fun browsing() = rule.collect(packageName = targetAppId, maxIterations = ITERATIONS) {
        startApp()
        browse()
    }

    private companion object {
        /** A long journey settles after a few rounds. */
        const val ITERATIONS = 6
    }
}
