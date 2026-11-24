import androidx.room3.gradle.RoomExtension
import io.github.amibhai.liftlog.buildlogic.libs
import io.github.amibhai.liftlog.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room 3 with KSP, exported (committed) schemas and the bundled SQLite driver. */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room3")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                // api: the database class extends RoomDatabase, so consumers need Room on their classpath.
                "api"(libs.library("room-runtime"))
                "implementation"(libs.library("sqlite-bundled"))
                "ksp"(libs.library("room-compiler"))
                "testImplementation"(libs.library("room-testing"))
            }
        }
    }
}
