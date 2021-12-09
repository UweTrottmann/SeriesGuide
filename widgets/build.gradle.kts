plugins {
    id("com.android.library")
    id("kotlin-android")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

val kotlinVersion: String by rootProject.extra
val annotationVersion: String by rootProject.extra
val fragmentVersion: String by rootProject.extra
val timberVersion: String by rootProject.extra

android {
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

    implementation("androidx.annotation:annotation:$annotationVersion")
    // https://developer.android.com/jetpack/androidx/releases/swiperefreshlayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager:viewpager:1.0.0")
    // https://developer.android.com/jetpack/androidx/releases/viewpager2
    // Note: override fragment version by viewpager2 to avoid Lint error.
    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")
    // 1.1.0-alpha01+ fixes issue with options menus from all fragments showing at once.
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta01")

    implementation("com.jakewharton.timber:timber:$timberVersion")
}
