import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    id("seriesguide.android")
    kotlin("android")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.uwetrottmann.seriesguide.widgets"

    defaultConfig {
        // Note: common settings configured by "seriesguide.android" plugin

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager)
    // Note: override fragment version by viewpager2 to avoid Lint error.
    implementation(libs.androidx.fragment)
    // 1.1.0-alpha01+ fixes issue with options menus from all fragments showing at once.
    implementation(libs.androidx.viewpager2)

    implementation(libs.timber)
}
