package io.github.amibhai.liftlog.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder

/** SDK levels, Java level, unit-test resources and strict lint for every Android module. */
internal fun configureAndroidCommon(extension: CommonExtension) {
    extension.compileSdk = LiftLogBuild.COMPILE_SDK
    extension.compileSdkMinor = LiftLogBuild.COMPILE_SDK_MINOR
    extension.defaultConfig.minSdk = LiftLogBuild.MIN_SDK

    extension.compileOptions.sourceCompatibility = LiftLogBuild.javaVersion
    extension.compileOptions.targetCompatibility = LiftLogBuild.javaVersion

    // Robolectric needs merged resources and assets (e.g. the prepopulated database).
    extension.testOptions.unitTests.isIncludeAndroidResources = true

    extension.lint.apply {
        warningsAsErrors = true
        abortOnError = true
        // "A newer version is available" checks would fail the build whenever a library ships.
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "NewerVersionAvailable",
            "OldTargetApi",
        )
    }
}

/** `./gradlew test` should run each suite once (debug), not once per build type. */
internal fun AndroidComponentsExtension<*, *, *>.disableReleaseUnitTests() {
    beforeVariants(selector().all()) { variantBuilder ->
        if (variantBuilder.buildType != "debug") {
            (variantBuilder as? HasHostTestsBuilder)
                ?.hostTests
                ?.get(HostTestBuilder.UNIT_TEST_TYPE)
                ?.enable = false
        }
    }
}
