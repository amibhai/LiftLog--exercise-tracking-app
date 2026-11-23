import io.github.amibhai.liftlog.buildlogic.libs
import io.github.amibhai.liftlog.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * A feature screen module: Compose UI + Hilt ViewModels + type-safe navigation routes.
 * Features talk to data only through `core:domain` interfaces.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "liftlog.android.library")
            apply(plugin = "liftlog.android.compose")
            apply(plugin = "liftlog.hilt")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:ui"))
                "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
                "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-navigation-compose"))
                "implementation"(libs.library("kotlinx-serialization-json"))

                "testImplementation"(project(":core:testing"))
            }

            enforceFeatureBoundaries()
        }
    }

    private fun Project.enforceFeatureBoundaries() {
        val forbidden = setOf("api", "implementation", "compileOnly", "runtimeOnly")
        configurations.matching { it.name in forbidden }.configureEach {
            dependencies.withType<ProjectDependency>().configureEach {
                val dependencyPath = path
                check(dependencyPath != ":core:database" && !dependencyPath.startsWith(":feature:")) {
                    "${this@enforceFeatureBoundaries.path} must not depend on $dependencyPath: " +
                        "features use core:domain interfaces and never depend on other features."
                }
            }
        }
    }
}
