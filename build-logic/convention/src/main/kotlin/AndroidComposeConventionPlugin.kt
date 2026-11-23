import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import io.github.amibhai.liftlog.buildlogic.libs
import io.github.amibhai.liftlog.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Apply after `liftlog.android.application` or `liftlog.android.library`. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            when {
                pluginManager.hasPlugin("com.android.application") ->
                    extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
                pluginManager.hasPlugin("com.android.library") ->
                    extensions.configure<LibraryExtension> { buildFeatures.compose = true }
                else -> error("$path: apply an Android convention plugin before liftlog.android.compose")
            }

            dependencies {
                val bom = platform(libs.library("androidx-compose-bom"))
                "implementation"(bom)
                "testImplementation"(bom)
                "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
                "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
            }

            extensions.configure<ComposeCompilerGradlePluginExtension> {
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("config/compose/stability.conf"),
                )
                // ./gradlew assembleRelease -PcomposeReports=true
                val reportsEnabled = providers.gradleProperty("composeReports").map(String::toBoolean).getOrElse(false)
                if (reportsEnabled) {
                    reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
                    metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
                }
            }
        }
    }
}
