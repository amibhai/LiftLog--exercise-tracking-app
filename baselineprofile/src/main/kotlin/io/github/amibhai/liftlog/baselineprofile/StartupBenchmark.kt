package io.github.amibhai.liftlog.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** How long a cold start takes (SPEC §13: less than a second), with and without the baseline profile. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun withoutProfile() = measure(CompilationMode.None())

    @Test
    fun withProfile() = measure(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun measure(compilation: CompilationMode) = rule.measureRepeated(
        packageName = targetAppId,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
        setupBlock = { pressHome() },
        measureBlock = { startActivityAndWait() },
    )

    private companion object {
        const val ITERATIONS = 10
    }
}
