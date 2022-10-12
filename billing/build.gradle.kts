plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

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
    implementation(libs.billing)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    // Room
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)

    implementation(libs.timber)
}

fun propertyOrEmpty(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "\"\""
}
