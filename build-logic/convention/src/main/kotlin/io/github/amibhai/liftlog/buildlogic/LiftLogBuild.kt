package io.github.amibhai.liftlog.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** Build-wide constants shared by every convention plugin. */
internal object LiftLogBuild {
    const val NAMESPACE_ROOT = "io.github.amibhai.liftlog"

    const val MIN_SDK = 26

    // Compose UI 1.13 declares minCompileSdk 37 / minCompileMinorSdk 1 in its AAR metadata.
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 1
    const val TARGET_SDK = 37

    val javaVersion: JavaVersion = JavaVersion.VERSION_17
    val jvmTarget: JvmTarget = JvmTarget.JVM_17

    /** `:core:domain` -> `io.github.amibhai.liftlog.core.domain`. */
    fun namespaceFor(projectPath: String): String = NAMESPACE_ROOT + projectPath.replace(':', '.')
}
