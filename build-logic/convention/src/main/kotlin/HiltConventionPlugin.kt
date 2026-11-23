import io.github.amibhai.liftlog.buildlogic.libs
import io.github.amibhai.liftlog.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Hilt with KSP for Android modules. Apply after an Android convention plugin. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")
            apply(plugin = "com.google.dagger.hilt.android")

            dependencies {
                "implementation"(libs.library("hilt-android"))
                "ksp"(libs.library("hilt-compiler"))
            }

            // The Hilt plugin adds generated sources to the unit-test source set even in modules
            // that have no tests yet, which Gradle 9 otherwise reports as a misconfigured test task.
            tasks.withType<Test>().configureEach {
                failOnNoDiscoveredTests.set(false)
            }
        }
    }
}
