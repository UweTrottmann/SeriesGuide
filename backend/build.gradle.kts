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
    // https://github.com/googleapis/google-api-java-client/releases
    val googleApiClientVersion = "1.33.4"
    api("com.google.api-client:google-api-client-android:$googleApiClientVersion") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
    }
    api("com.google.api-client:google-api-client:$googleApiClientVersion") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
    }
    // https://github.com/googleapis/google-http-java-client/releases
    api("com.google.http-client:google-http-client-gson:1.41.5") {
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
