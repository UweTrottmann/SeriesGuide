import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("android")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.uwetrottmann.seriesguide.widgets"
    compileSdk = sgCompileSdk

    defaultConfig {
        minSdk = sgMinSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
