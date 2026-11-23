package io.github.amibhai.liftlog.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** JVM target and "no compiler warnings" (SPEC §1) for every Kotlin compilation. */
internal fun Project.configureKotlin() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(LiftLogBuild.jvmTarget)
            allWarningsAsErrors.set(true)
        }
    }
}
