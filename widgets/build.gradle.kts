import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("android")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra

tasks.withType(JavaCompile::class.java).configureEach {
    // Suppress JDK 21 warning about deprecated, but not yet removed, source and target value 8 support
    options.compilerArgs.add("-Xlint:-options")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
