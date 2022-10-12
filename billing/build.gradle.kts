plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

val lifecycleVersion: String by rootProject.extra
val roomVersion: String by rootProject.extra
val timberVersion: String by rootProject.extra

android {
    namespace = "com.uwetrottmann.seriesguide.billing"
    compileSdk = sgCompileSdk

    defaultConfig {
        minSdk = sgMinSdk
        targetSdk = sgTargetSdk

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
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Google Play Billing
    // https://developer.android.com/google/play/billing/billing_library_releases_notes
    implementation("com.android.billingclient:billing-ktx:4.0.0")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("com.jakewharton.timber:timber:$timberVersion")
}

fun propertyOrEmpty(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "\"\""
}
