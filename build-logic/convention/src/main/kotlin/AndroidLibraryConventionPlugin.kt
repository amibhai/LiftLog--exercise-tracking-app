import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import io.github.amibhai.liftlog.buildlogic.LiftLogBuild
import io.github.amibhai.liftlog.buildlogic.configureAndroidCommon
import io.github.amibhai.liftlog.buildlogic.configureKotlin
import io.github.amibhai.liftlog.buildlogic.disableReleaseUnitTests
import io.github.amibhai.liftlog.buildlogic.libs
import io.github.amibhai.liftlog.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "liftlog.detekt")

            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                namespace = LiftLogBuild.namespaceFor(path)
                testOptions.targetSdk = LiftLogBuild.TARGET_SDK
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                disableReleaseUnitTests()
            }
            configureKotlin()

            dependencies {
                "testImplementation"(libs.library("junit4"))
                "testImplementation"(libs.library("truth"))
            }
        }
    }
}
