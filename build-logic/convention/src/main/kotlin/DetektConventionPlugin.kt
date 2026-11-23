import dev.detekt.gradle.extensions.DetektExtension
import io.github.amibhai.liftlog.buildlogic.libs
import io.github.amibhai.liftlog.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** detekt 2 with the ktlint wrapper (formatting) and compose-rules rule sets. */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "dev.detekt")

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)
                config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
                basePath.set(rootProject.layout.projectDirectory)
                parallel.set(true)
            }

            dependencies {
                "detektPlugins"(libs.library("detekt-rules-ktlint-wrapper"))
                "detektPlugins"(libs.library("compose-rules-detekt"))
            }
        }
    }
}
