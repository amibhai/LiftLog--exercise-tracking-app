plugins {
    alias(libs.plugins.liftlog.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
