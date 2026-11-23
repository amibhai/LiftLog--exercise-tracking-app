import io.github.amibhai.liftlog.buildlogic.LiftLogBuild
import io.github.amibhai.liftlog.buildlogic.configureKotlin
import io.github.amibhai.liftlog.buildlogic.libs
import io.github.amibhai.liftlog.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Pure Kotlin/JVM module (no Android) tested with JUnit Jupiter + Truth. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "liftlog.detekt")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = LiftLogBuild.javaVersion
                targetCompatibility = LiftLogBuild.javaVersion
            }
            configureKotlin()

            dependencies {
                "testImplementation"(platform(libs.library("junit-bom")))
                "testImplementation"(libs.library("junit-jupiter"))
                "testImplementation"(libs.library("truth"))
                "testRuntimeOnly"(libs.library("junit-platform-launcher"))
            }
            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
