import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
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

android {
    namespace = "com.battlelancer.seriesguide"
    compileSdk = sgCompileSdk

    useLibrary("android.test.base")

    buildFeatures {
        buildConfig = true
        // https://developer.android.com/jetpack/compose/interop/adding
        compose = true
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

        // Note: do not exclude languages from libraries that the app doesn't have, e.g. Firebase Auth.
        // They still might be helpful to users, e.g. for regional dialects.
        // resourceConfigurations
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

    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.8" // For Kotlin 1.9.22
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        // Using experimental flatMapLatest for Paging 3
        // Using experimental Material 3 compose APIs
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi,androidx.compose.material3.ExperimentalMaterial3Api"
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
            isDefault = true // Make Studio select this by default, it often resets (after updates, randomly)

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

    packaging {
        resources {
            // google-auth-library-oauth2-http and google-auth-library-credentials include INDEX.LIST:
            // Based on https://docs.oracle.com/en/java/javase/17/docs/specs/jar/jar.html#jar-index
            // only used by network applications like applets, so safe to exclude.
            excludes += "/META-INF/INDEX.LIST"
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.kotlinx.result)

    implementation(project(":api"))
    implementation(project(":backend"))
    implementation(project(":billing"))
    implementation(project(":widgets"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.collection) // For SparseArrayCompat
    implementation(libs.androidx.fragment)
    implementation(libs.material)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.swiperefreshlayout)

    // Compose
    implementation(platform(libs.compose))
    androidTestImplementation(platform(libs.compose))
    implementation(libs.compose.material3)
    // Android Studio Preview support
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    // Optional - Integration with ViewModels
    implementation( libs.androidx.lifecycle.compose)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)

    // Paging
    implementation(libs.androidx.paging)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Paging 3 Integration
    implementation(libs.androidx.room.paging)
    kapt(libs.androidx.room.compiler)

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)
    implementation(libs.greenrobot.eventbus)
    kapt(libs.greenrobot.eventbus.processor)

    implementation(libs.flatbuffers)
    implementation(libs.gson)
    implementation(libs.threetenabp)
    implementation(libs.timber)

    // Use latest OkHttp.
    implementation(libs.okhttp)
    // Use latest retrofit.
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.gson)

    implementation(libs.picasso)

    implementation(libs.androidutils)
    implementation(libs.photoview)

    implementation(libs.tmdb.java)
    implementation(libs.trakt.java) {
        exclude(group = "org.threeten", module = "threetenbp") // using ThreeTenABP instead
    }

    implementation(libs.debugdrawer.base)
    implementation(libs.debugdrawer.view)
    implementation(libs.debugdrawer.commons)
    implementation(libs.debugdrawer.actions)
    implementation(libs.debugdrawer.timber)

    // Note: can not use Firebase BOM as firebase-ui-auth has not updated in a while
    // Crashlytics
    implementation(libs.firebase.crashlytics)
    // Firebase Sign-In
    implementation(libs.firebase.ui.auth)
    // Use compatible later versions of firebase-ui-auth dependencies to get latest fixes.
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    // Amazon flavor specific
    // Note: requires to add AppstoreAuthenticationKey.pem into amazon/assets.
    "amazonImplementation"(libs.amazon.appstore.sdk)

    // Instrumented unit tests
    androidTestImplementation(libs.androidx.annotation)
    // Core library
    androidTestImplementation(libs.androidx.test.core)
    // AndroidJUnitRunner and JUnit Rules
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    // Espresso
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.contrib) {
        // conflicts with checker-qual from guava transitive dependency
        exclude(group = "org.checkerframework", module = "checker")
    }
    // Assertions
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.truth) {
        exclude(group = "com.google.truth") // include manually to control conflicting deps
    }
    androidTestImplementation(libs.truth)
    implementation(libs.findbugs.jsr305)
    androidTestImplementation(libs.findbugs.jsr305)
    kaptAndroidTest(libs.dagger.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    // Local unit tests
    testImplementation(libs.junit)
    testImplementation(libs.androidx.annotation)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // https://github.com/mockito/mockito/releases
    testImplementation(libs.mockito)
    testImplementation(libs.kotlinx.coroutines.test)
}

fun propertyOrEmpty(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "\"\""
}

fun propertyOrNull(name: String): String {
    return if (rootProject.hasProperty(name)) "\"${rootProject.property(name)}\"" else "null"
}
