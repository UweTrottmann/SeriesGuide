plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.cloud.tools.endpoints-framework-client")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

android {
    namespace = "com.uwetrottmann.seriesguide.backend"
    compileSdk = sgCompileSdk

    defaultConfig {
        minSdk = sgMinSdk
        targetSdk = sgTargetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    // App Engine
    api(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
    }
    api(libs.google.api.client) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
    }
    api(libs.google.http.client) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
    }
}

endpointsClient {
    setDiscoveryDocs(
        listOf(
            "src/endpoints/account-v2-rest.discovery",
            "src/endpoints/episodes-v2-rest.discovery",
            "src/endpoints/lists-v2-rest.discovery",
            "src/endpoints/movies-v2-rest.discovery",
            "src/endpoints/shows-v2-rest.discovery"
        )
    )
}
