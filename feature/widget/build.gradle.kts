plugins {
    alias(libs.plugins.liftlog.android.feature)
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.androidx.glance.appwidget.testing)
}
