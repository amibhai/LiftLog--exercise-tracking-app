import java.util.Properties

plugins {
    alias(libs.plugins.liftlog.android.application)
    alias(libs.plugins.liftlog.android.compose)
    alias(libs.plugins.liftlog.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Release builds are signed with your own key when keystore.properties exists (never committed),
// otherwise with the debug key so debug and release can replace each other on the phone.
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "io.github.amibhai.liftlog"

    defaultConfig {
        applicationId = "io.github.amibhai.liftlog"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.feature.exercises)
    implementation(projects.feature.history)
    implementation(projects.feature.home)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.progress)
    implementation(projects.feature.routines)
    implementation(projects.feature.settings)
    implementation(projects.feature.widget)
    implementation(projects.feature.workout)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
