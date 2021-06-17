plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

val kotlin_version: String by rootProject.extra
val coroutines_version: String by rootProject.extra
val lifecycle_version: String by rootProject.extra
val room_version: String by rootProject.extra
val timber_version: String by rootProject.extra

android {
    compileSdk = sgCompileSdk

    defaultConfig {
        minSdk = sgMinSdk
        targetSdk = sgTargetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "IAP_KEY_A", propertyOrEmpty("SG_IAP_KEY_A"))
        buildConfigField("String", "IAP_KEY_B", propertyOrEmpty("SG_IAP_KEY_B"))
        buildConfigField("String", "IAP_KEY_C", propertyOrEmpty("SG_IAP_KEY_C"))
        buildConfigField("String", "IAP_KEY_D", propertyOrEmpty("SG_IAP_KEY_D"))
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    // Google Play Billing
    // https://developer.android.com/google/play/billing/billing_library_releases_notes
    implementation("com.android.billingclient:billing-ktx:4.0.0")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    // Room
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    implementation("com.jakewharton.timber:timber:$timber_version")
}

fun propertyOrEmpty(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "\"\""
}
