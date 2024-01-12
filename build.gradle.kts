// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.versions)
    // app, libraries
    alias(libs.plugins.android) apply false
    alias(libs.plugins.kotlin) apply false
    // Firebase Crashlytics
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    // Cloud Endpoints
    alias(libs.plugins.endpoints) apply false
    // api
    alias(libs.plugins.publish)
}

buildscript {
    val sgCompileSdk by extra(34) // Android 14 (U)
    val sgMinSdk by extra(21) // Android 5 (L)
    val sgTargetSdk by extra(33) // Android 13 (T)

    // version 21xxxyy -> min SDK 21, release xxx, build yy
    val sgVersionCode by extra(2107104)
    val sgVersionName by extra("71.0.3")

    val isCiBuild by extra { System.getenv("CI") == "true" }

    // load some properties that should not be part of version control
    if (file("secret.properties").exists()) {
        val properties = java.util.Properties()
        properties.load(java.io.FileInputStream(file("secret.properties")))
        properties.forEach { property ->
            project.extra.set(property.key as String, property.value)
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
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
    this.repositories {
        sonatype {
            if (rootProject.hasProperty("SONATYPE_NEXUS_USERNAME")
                && rootProject.hasProperty("SONATYPE_NEXUS_PASSWORD")) {
                username.set(rootProject.property("SONATYPE_NEXUS_USERNAME").toString())
                password.set(rootProject.property("SONATYPE_NEXUS_PASSWORD").toString())
            }
        }
    }
}

tasks.wrapper {
    //noinspection UnnecessaryQualifiedReference
    distributionType = Wrapper.DistributionType.ALL
}
