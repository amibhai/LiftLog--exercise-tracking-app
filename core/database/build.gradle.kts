plugins {
    alias(libs.plugins.liftlog.android.library)
    alias(libs.plugins.liftlog.android.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    sourceSets {
        // MigrationTestHelper reads the exported schemas from the test assets.
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    // Robolectric runs on the host JVM, where the bundled driver's Android .so files cannot load,
    // so tests use the framework driver backed by Robolectric's native SQLite.
    testImplementation(libs.sqlite.framework)
}
