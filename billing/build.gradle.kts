plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra

tasks.withType(JavaCompile::class.java).configureEach {
    // Suppress JDK 21 warning about deprecated, but not yet removed, source and target value 8 support
    options.compilerArgs.add("-Xlint:-options")
}

android {
    namespace = "com.uwetrottmann.seriesguide.billing"
    compileSdk = sgCompileSdk

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = sgMinSdk

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
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Google Play Billing
    api(libs.billing)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    // Room
    implementation(libs.androidx.room.runtime)
    // KSP appears deprecated. KSP 2 is still under development.
    //noinspection KaptUsageInsteadOfKsp
    kapt(libs.androidx.room.compiler)

    implementation(libs.timber)
}

fun propertyOrEmpty(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "\"\""
}
