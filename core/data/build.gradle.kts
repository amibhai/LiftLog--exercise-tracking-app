plugins {
    alias(libs.plugins.liftlog.android.library)
    alias(libs.plugins.liftlog.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.database)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
