import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.cloud.tools.endpoints-framework-client")
    id("com.google.firebase.crashlytics")
}

if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

val sgVersionCode: Int by rootProject.extra
val sgVersionName: String by rootProject.extra

val kotlin_version: String by rootProject.extra
val coroutines_version: String by rootProject.extra

val core_version: String by rootProject.extra
val annotation_version: String by rootProject.extra
val lifecycle_version: String by rootProject.extra
val room_version: String by rootProject.extra
val fragmentVersion: String by rootProject.extra

val dagger_version: String by rootProject.extra
val okhttp_version: String by rootProject.extra
val retrofit_version: String by rootProject.extra
val timber_version: String by rootProject.extra

android {
    compileSdk = sgCompileSdk

    useLibrary("android.test.base")

    buildFeatures {
        // https://firebase.google.com/support/release-notes/android
        viewBinding = true
    }

    defaultConfig {
        minSdk = sgMinSdk
        targetSdk = sgTargetSdk

        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TMDB_API_KEY", propertyOrEmpty("SG_TMDB_API_KEY"))
        buildConfigField("String", "TRAKT_CLIENT_ID", propertyOrEmpty("SG_TRAKT_CLIENT_ID"))
        buildConfigField("String", "TRAKT_CLIENT_SECRET", propertyOrEmpty("SG_TRAKT_CLIENT_SECRET"))
        buildConfigField("String", "IMAGE_CACHE_URL", propertyOrNull("SG_IMAGE_CACHE_URL"))
        buildConfigField("String", "IMAGE_CACHE_SECRET", propertyOrEmpty("SG_IMAGE_CACHE_SECRET"))
        buildConfigField("String", "COUNT_URL", propertyOrNull("SG_COUNT_URL"))
        buildConfigField("String", "COUNT_SECRET", propertyOrEmpty("SG_COUNT_SECRET"))

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["eventBusIndex"] = "com.battlelancer.seriesguide.SgEventBusIndex"
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
            }
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    lintOptions {
        // for CI server: only check this module with dependencies instead of each module separately
        isCheckDependencies = true
        // for CI server: log reports (report files are not public)
        textReport = true
        textOutput("stdout")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    flavorDimensions("flavor")

    productFlavors {
        create("pure") {
            applicationId = "com.battlelancer.seriesguide"
            versionCode = sgVersionCode
            versionName = sgVersionName

            val backupKey = if (rootProject.hasProperty("SG_ANDROID_BACKUP_KEY")) {
                rootProject.property("SG_ANDROID_BACKUP_KEY").toString()
            } else "MISSING"
            manifestPlaceholders["androidBackupKey"] = backupKey
        }
        create("amazon") {
            applicationId = "com.uwetrottmann.seriesguide.amzn"
            versionCode = sgVersionCode
            versionName = sgVersionName
        }
    }

    signingConfigs {
        create("release") {
            if (rootProject.file("keystore.properties").exists()) {
                val props = Properties()
                props.load(FileInputStream(rootProject.file("keystore.properties")))

                storeFile = file(props["storeFile"]!!)
                storePassword = props["storePassword"]!!.toString()
                keyAlias = props["keyAlias"]!!.toString()
                keyPassword = props["keyPassword"]!!.toString()
            }
        }
    }

    buildTypes {
        getByName("release") {
            multiDexEnabled = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            multiDexEnabled = true
            // disable shrinking to use incremental dex in builds
            isMinifyEnabled = false
            // en_XA (LTR) and ar_XB (RTL) to test UI adjusting to unusual glyphs and long strings
            // keep disabled unless needed, slows down build
            isPseudoLocalesEnabled = false
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    implementation(project(":api"))
    implementation(project(":billing"))
    implementation(project(":widgets"))

    implementation("androidx.core:core-ktx:$core_version")
    implementation("androidx.annotation:annotation:$annotation_version")
    // https://developer.android.com/jetpack/androidx/releases/appcompat
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    // https://developer.android.com/jetpack/androidx/releases/browser
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")
    // https://github.com/material-components/material-components-android/releases
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    // https://developer.android.com/jetpack/androidx/releases/recyclerview
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.preference:preference-ktx:1.1.1")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    // Paging
    implementation("androidx.paging:paging-runtime-ktx:2.1.2")

    // Room
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // https://github.com/JakeWharton/butterknife/blob/master/CHANGELOG.md
    val butterknife_version = "10.2.3"
    implementation("com.jakewharton:butterknife:$butterknife_version")
    kapt("com.jakewharton:butterknife-compiler:$butterknife_version")
    implementation("com.google.dagger:dagger:$dagger_version")
    kapt("com.google.dagger:dagger-compiler:$dagger_version")
    val eventbus_version = "3.2.0"
    implementation("org.greenrobot:eventbus:$eventbus_version")
    kapt("org.greenrobot:eventbus-annotation-processor:$eventbus_version")

    implementation("com.google.flatbuffers:flatbuffers-java:1.12.0")
    // https://github.com/google/gson/blob/master/CHANGELOG.md
    implementation("com.google.code.gson:gson:2.8.6")
    // https://github.com/JakeWharton/ThreeTenABP/blob/master/CHANGELOG.md
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.1")
    implementation("com.jakewharton.timber:timber:$timber_version")
    implementation("com.readystatesoftware.systembartint:systembartint:1.0.4")

    // Use latest OkHttp.
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    // Use latest retrofit.
    implementation("com.squareup.retrofit2:retrofit:$retrofit_version")
    implementation("com.squareup.retrofit2:converter-gson:$retrofit_version")

    implementation("com.squareup.picasso:picasso:2.71828")

    // https://github.com/UweTrottmann/AndroidUtils/blob/master/RELEASE_NOTES.md
    implementation("com.uwetrottmann.androidutils:androidutils:2.4.1") {
        exclude(group = "com.android.support")
    }
    implementation("com.uwetrottmann.photoview:library:1.2.4")

    // https://github.com/UweTrottmann/tmdb-java/blob/master/CHANGELOG.md
    implementation("com.uwetrottmann.tmdb2:tmdb-java:2.3.1")
    // https://github.com/UweTrottmann/trakt-java/blob/master/CHANGELOG.md
    implementation("com.uwetrottmann.trakt5:trakt-java:6.9.0") {
        exclude(group = "org.threeten", module = "threetenbp") // using ThreeTenABP instead
    }

    // https://github.com/lenguyenthanh/DebugDrawer
    val debugDrawerVersion = "0.9.0"
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-base:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-view:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-commons:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-actions:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-timber:$debugDrawerVersion")

    // Crashlytics
    // https://firebase.google.com/support/release-notes/android
    implementation("com.google.firebase:firebase-crashlytics:17.4.0")

    // Countly https://github.com/Countly/countly-sdk-android/releases
    implementation("ly.count.android:sdk:20.11.8")

    // Google Play Services
    // https://developers.google.com/android/guides/releases
    implementation("com.google.android.gms:play-services-auth:19.0.0")

    // App Engine
    // https://github.com/googleapis/google-api-java-client/releases
    // Note: 1.31.5 has broken dependencies.
    implementation("com.google.api-client:google-api-client-android:1.31.2") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }

    // Amazon flavor specific
    "amazonImplementation"(files("libs/amazon/in-app-purchasing-2.0.76.jar"))

    // Instrumented unit tests
    // https://developer.android.com/jetpack/androidx/releases/test
    androidTestImplementation("androidx.annotation:annotation:$annotation_version")
    // Core library
    val androidXtestCoreVersion = "1.3.0"
    androidTestImplementation("androidx.test:core:$androidXtestCoreVersion")
    // AndroidJUnitRunner and JUnit Rules
    androidTestImplementation("androidx.test:runner:1.3.0")
    androidTestImplementation("androidx.test:rules:1.3.0")
    // Espresso
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.3.0")
    // Assertions
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.ext:truth:1.3.0") {
        exclude(group = "com.google.truth") // include manually to control conflicting deps
    }
    val truthVersion = "1.1.2" // https://github.com/google/truth/releases
    androidTestImplementation("com.google.truth:truth:$truthVersion") {
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    androidTestImplementation("com.google.code.findbugs:jsr305:3.0.2")
    kaptAndroidTest("com.google.dagger:dagger-compiler:$dagger_version")
    androidTestImplementation("androidx.room:room-testing:$room_version")

    // Local unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.annotation:annotation:$annotation_version")
    testImplementation("com.google.truth:truth:$truthVersion") {
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }
    // https://github.com/robolectric/robolectric/releases/
    testImplementation("org.robolectric:robolectric:4.5.1")
    testImplementation("androidx.test:core:$androidXtestCoreVersion")

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

fun propertyOrEmpty(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "\"\""
}

fun propertyOrNull(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "null"
}
