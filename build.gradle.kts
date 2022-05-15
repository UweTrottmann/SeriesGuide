// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.42.0"
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0" // api
}

buildscript {
    val sgCompileSdk by extra(31) // Android 12 (S)
    val sgMinSdk by extra(21) // Android 5 (L)
    val sgTargetSdk by extra(31) // Android 12 (S)

    // version 21xxxyy -> min SDK 21, release xxx, build yy
    val sgVersionCode by extra(2106406)
    val sgVersionName by extra("64.0.6")

    val kotlinVersion by extra("1.6.10") // https://kotlinlang.org/docs/releases.html#release-details
    val coroutinesVersion by extra("1.6.0") // https://github.com/Kotlin/kotlinx.coroutines/blob/master/CHANGES.md

    // https://developer.android.com/jetpack/androidx/releases
    val coreVersion by extra("1.7.0") // https://developer.android.com/jetpack/androidx/releases/core
    val annotationVersion by extra("1.3.0")
    val lifecycleVersion by extra("2.4.0")
    val roomVersion by extra("2.4.1") // https://developer.android.com/jetpack/androidx/releases/room
    val fragmentVersion by extra("1.4.1") // https://developer.android.com/jetpack/androidx/releases/fragment

    val timberVersion by extra("5.0.1") // https://github.com/JakeWharton/timber/blob/master/CHANGELOG.md

    val isCiBuild by extra { System.getenv("CI") == "true" }

    // load some properties that should not be part of version control
    if (file("secret.properties").exists()) {
        val properties = java.util.Properties()
        properties.load(java.io.FileInputStream(file("secret.properties")))
        properties.forEach { property ->
            project.extra.set(property.key as String, property.value)
        }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.2.0") // libraries, SeriesGuide
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.cloud.tools:endpoints-framework-gradle-plugin:2.1.0") // SeriesGuide
        // Firebase Crashlytics
        // https://firebase.google.com/support/release-notes/android
        classpath("com.google.gms:google-services:4.3.10")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.1")
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

nexusPublishing {
    packageGroup.set("com.uwetrottmann")
    repositories {
        sonatype {
            if (rootProject.hasProperty("SONATYPE_NEXUS_USERNAME")
                && rootProject.hasProperty("SONATYPE_NEXUS_PASSWORD")) {
                username.set(rootProject.property("SONATYPE_NEXUS_USERNAME").toString())
                password.set(rootProject.property("SONATYPE_NEXUS_PASSWORD").toString())
            }
        }
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
