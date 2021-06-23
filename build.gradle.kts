// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.38.0"
    // https://github.com/Codearte/gradle-nexus-staging-plugin/releases
    id("io.codearte.nexus-staging") version "0.22.0" // api
}

buildscript {
    val sgCompileSdk by extra(30) // Android 11 (R)
    val sgMinSdk by extra(21) // Android 5 (L)
    val sgTargetSdk by extra(30) // Android 11 (R)

    // version 21xxxyy -> min SDK 21, release xxx, build yy
    val sgVersionCode by extra(2105904)
    val sgVersionName by extra("59.1")

    val kotlin_version by extra("1.5.0") // https://kotlinlang.org/docs/releases.html#release-details
    val coroutines_version by extra("1.5.0") // https://github.com/Kotlin/kotlinx.coroutines/blob/master/CHANGES.md

    // https://developer.android.com/jetpack/androidx/releases
    val core_version by extra("1.5.0") // https://developer.android.com/jetpack/androidx/releases/core
    val annotation_version by extra("1.2.0")
    val lifecycle_version by extra("2.3.1")
    val room_version by extra("2.3.0") // https://developer.android.com/jetpack/androidx/releases/room

    val dagger_version by extra("2.35.1") // https://github.com/google/dagger/releases
    val okhttp_version by extra("4.9.1") // https://github.com/square/okhttp/blob/master/CHANGELOG.md
    val retrofit_version by extra("2.9.0") // https://github.com/square/retrofit/blob/master/CHANGELOG.md
    val timber_version by extra("4.7.1") // https://github.com/JakeWharton/timber/blob/master/CHANGELOG.md

    val isCiBuild by extra { System.getenv("CI") == "true" }

    // load some properties that should not be part of version control
    if (file("secret.properties").exists()) {
        val properties = java.util.Properties()
        properties.load(java.io.FileInputStream(file("secret.properties")))
        properties.forEach { property ->
            project.extra.set(property.key as String, property.value)
        }
    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.0") // libraries, SeriesGuide
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("com.google.cloud.tools:endpoints-framework-gradle-plugin:2.1.0") // SeriesGuide
        // Firebase Crashlytics
        // https://firebase.google.com/support/release-notes/android
        classpath("com.google.gms:google-services:4.3.5")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.5.1")
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

nexusStaging {
    packageGroup = "com.uwetrottmann"
    if (rootProject.hasProperty("SONATYPE_NEXUS_USERNAME")
            && rootProject.hasProperty("SONATYPE_NEXUS_PASSWORD")) {
        username = rootProject.property("SONATYPE_NEXUS_USERNAME").toString()
        password = rootProject.property("SONATYPE_NEXUS_PASSWORD").toString()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    group = "build"
    delete(rootProject.buildDir)
}

tasks.wrapper {
    //noinspection UnnecessaryQualifiedReference
    distributionType = Wrapper.DistributionType.ALL
}
