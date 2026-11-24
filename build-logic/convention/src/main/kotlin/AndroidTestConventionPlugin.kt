import com.android.build.api.dsl.TestExtension
import io.github.amibhai.liftlog.buildlogic.LiftLogBuild
import io.github.amibhai.liftlog.buildlogic.configureAndroidCommon
import io.github.amibhai.liftlog.buildlogic.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/** A module that drives the installed app from the outside: benchmarks and the baseline profile. */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.test")
            apply(plugin = "liftlog.detekt")

            extensions.configure<TestExtension> {
                configureAndroidCommon(this)
                defaultConfig.targetSdk = LiftLogBuild.TARGET_SDK
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            configureKotlin()
        }
    }
}
