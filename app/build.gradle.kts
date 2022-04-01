import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.cloud.tools.endpoints-framework-client")
}

if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
// Note: need to apply Crashlytics after Google services plugin,
// above conditional apply doesn't work inside plugins block.
apply(plugin = "com.google.firebase.crashlytics")

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

val sgVersionCode: Int by rootProject.extra
val sgVersionName: String by rootProject.extra

val kotlinVersion: String by rootProject.extra
val coroutinesVersion: String by rootProject.extra

val coreVersion: String by rootProject.extra
val annotationVersion: String by rootProject.extra
val lifecycleVersion: String by rootProject.extra
val roomVersion: String by rootProject.extra
val fragmentVersion: String by rootProject.extra

val timberVersion: String by rootProject.extra

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
        // Using experimental flatMapLatest for Paging 3
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }

    lint {
        // for CI server: only check this module with dependencies instead of each module separately
        checkDependencies = true
        // for CI server: log reports (report files are not public)
        textReport = true
        // Note: do not use textOutput = file("stdout"), just set no file.
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    flavorDimensions += listOf("flavor")

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

// Note: android.javaCompileOptions.annotationProcessorOptions does not seem to work with Kotlin 1.5.20
kapt {
    arguments {
        arg("eventBusIndex", "com.battlelancer.seriesguide.SgEventBusIndex")
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

    implementation(project(":api"))
    implementation(project(":billing"))
    implementation(project(":widgets"))

    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("androidx.annotation:annotation:$annotationVersion")
    // https://developer.android.com/jetpack/androidx/releases/appcompat
    implementation("androidx.appcompat:appcompat:1.4.1")
    // https://developer.android.com/jetpack/androidx/releases/browser
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")
    // https://github.com/material-components/material-components-android/releases
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    // https://developer.android.com/jetpack/androidx/releases/recyclerview
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.preference:preference-ktx:1.1.1")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.1.0")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // Paging 3 Integration
    implementation("androidx.room:room-paging:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // https://github.com/JakeWharton/butterknife/blob/master/CHANGELOG.md
    val butterknifeVersion = "10.2.3"
    implementation("com.jakewharton:butterknife:$butterknifeVersion")
    kapt("com.jakewharton:butterknife-compiler:$butterknifeVersion")
    // https://github.com/google/dagger/releases
    val daggerVersion  = "2.40.5"
    implementation("com.google.dagger:dagger:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")
    val eventbusVersion = "3.3.1"
    implementation("org.greenrobot:eventbus:$eventbusVersion")
    kapt("org.greenrobot:eventbus-annotation-processor:$eventbusVersion")

    implementation("com.google.flatbuffers:flatbuffers-java:1.12.0")
    // https://github.com/google/gson/blob/master/CHANGELOG.md
    implementation("com.google.code.gson:gson:2.9.0")
    // https://github.com/JakeWharton/ThreeTenABP/blob/master/CHANGELOG.md
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.1")
    implementation("com.jakewharton.timber:timber:$timberVersion")
    implementation("com.readystatesoftware.systembartint:systembartint:1.0.4")

    // Use latest OkHttp.
    // https://github.com/square/okhttp/blob/master/CHANGELOG.md
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    // Use latest retrofit.
    // https://github.com/square/retrofit/blob/master/CHANGELOG.md
    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")

    implementation("com.squareup.picasso:picasso:2.71828")

    // https://github.com/UweTrottmann/AndroidUtils/releases
    implementation("com.uwetrottmann.androidutils:androidutils:3.0.0")
    implementation("com.uwetrottmann.photoview:library:1.2.4")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")

    // https://github.com/UweTrottmann/tmdb-java/blob/master/CHANGELOG.md
    implementation("com.uwetrottmann.tmdb2:tmdb-java:2.6.0")
    // https://github.com/UweTrottmann/trakt-java/blob/master/CHANGELOG.md
    implementation("com.uwetrottmann.trakt5:trakt-java:6.10.0") {
        exclude(group = "org.threeten", module = "threetenbp") // using ThreeTenABP instead
    }

    // https://github.com/lenguyenthanh/DebugDrawer
    val debugDrawerVersion = "0.9.0"
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-base:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-view:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-commons:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-actions:$debugDrawerVersion")
    implementation("com.github.lenguyenthanh.debugdrawer:debugdrawer-timber:$debugDrawerVersion")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:29.0.1"))
    // Firebase Sign-In https://github.com/firebase/FirebaseUI-Android/releases
    implementation("com.firebaseui:firebase-ui-auth:8.0.0")
    // Update play-services-auth which adds Android 12 mutable Intent flags.
    // https://developers.google.com/android/guides/releases
    implementation("com.google.android.gms:play-services-auth:20.0.1")


    // Crashlytics
    // https://firebase.google.com/support/release-notes/android
    implementation("com.google.firebase:firebase-crashlytics:18.2.5")

    // App Engine
    // https://github.com/googleapis/google-api-java-client/releases
    val googleApiClientVersion = "1.33.4"
    implementation("com.google.api-client:google-api-client-android:$googleApiClientVersion") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }
    implementation("com.google.api-client:google-api-client:$googleApiClientVersion") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }
    // https://github.com/googleapis/google-http-java-client/releases
    implementation("com.google.http-client:google-http-client-gson:1.41.5") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient") // unused
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }

    // Amazon flavor specific
    // Note: requires to add AppstoreAuthenticationKey.pem into amazon/assets.
    "amazonImplementation"("com.amazon.device:amazon-appstore-sdk:3.0.2")

    // Instrumented unit tests
    // https://developer.android.com/jetpack/androidx/releases/test
    androidTestImplementation("androidx.annotation:annotation:$annotationVersion")
    // Core library
    val androidXtestCoreVersion = "1.4.0"
    androidTestImplementation("androidx.test:core:$androidXtestCoreVersion")
    // AndroidJUnitRunner and JUnit Rules
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    // Espresso
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.4.0")
    // Assertions
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.ext:truth:1.4.0") {
        exclude(group = "com.google.truth") // include manually to control conflicting deps
    }
    val truthVersion = "1.1.3" // https://github.com/google/truth/releases
    androidTestImplementation("com.google.truth:truth:$truthVersion") {
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    androidTestImplementation("com.google.code.findbugs:jsr305:3.0.2")
    kaptAndroidTest("com.google.dagger:dagger-compiler:$daggerVersion")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    // Local unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.annotation:annotation:$annotationVersion")
    testImplementation("com.google.truth:truth:$truthVersion") {
        exclude(group = "org.checkerframework") // from guava, not needed at runtime
        exclude(group = "com.google.errorprone") // from guava, not needed at runtime
    }
    // https://github.com/robolectric/robolectric/releases/
    // Note: 4.6.1 pulls in bcprov-jdk15on code targeting newer Java breaking Jetifier
    // Not fixed until Android Plugin 7 release. Ignore listed in gradle.properties.
    // https://github.com/robolectric/robolectric/issues/6521
    // https://issuetracker.google.com/issues/159151549
    testImplementation("org.robolectric:robolectric:4.7.3")
    testImplementation("androidx.test:core:$androidXtestCoreVersion")
    // https://github.com/mockito/mockito/releases
    testImplementation("org.mockito:mockito-core:4.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
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
