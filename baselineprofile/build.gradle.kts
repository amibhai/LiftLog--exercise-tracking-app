plugins {
    alias(libs.plugins.liftlog.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.amibhai.liftlog.baselineprofile"
    targetProjectPath = ":app"

    // Macrobenchmarks need Android 9.
    defaultConfig.minSdk = 28
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
}
