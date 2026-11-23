import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import io.github.amibhai.liftlog.buildlogic.LiftLogBuild
import io.github.amibhai.liftlog.buildlogic.configureAndroidCommon
import io.github.amibhai.liftlog.buildlogic.configureKotlin
import io.github.amibhai.liftlog.buildlogic.disableReleaseUnitTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "liftlog.detekt")

            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this)
                defaultConfig.targetSdk = LiftLogBuild.TARGET_SDK
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                disableReleaseUnitTests()
            }
            configureKotlin()
        }
    }
}
