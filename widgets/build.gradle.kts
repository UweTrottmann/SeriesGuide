plugins {
    id("com.android.library")
    kotlin("android")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

android {
    namespace = "com.uwetrottmann.seriesguide.widgets"
    compileSdk = sgCompileSdk

    defaultConfig {
        minSdk = sgMinSdk
        targetSdk = sgTargetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
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
