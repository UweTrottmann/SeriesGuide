// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.43.0"
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0" // api
}

buildscript {
    val sgCompileSdk by extra(33) // Android 13 (T)
    val sgMinSdk by extra(21) // Android 5 (L)
    val sgTargetSdk by extra(31) // Android 12 (S)

    // version 21xxxyy -> min SDK 21, release xxx, build yy
    val sgVersionCode by extra(2106600)
    val sgVersionName by extra("66.0.0")

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
        classpath("com.android.tools.build:gradle:7.3.1") // libraries, SeriesGuide
        classpath(libs.kotlin.gradle)
        classpath("com.google.cloud.tools:endpoints-framework-gradle-plugin:2.1.0") // SeriesGuide
        // Firebase Crashlytics
        classpath(libs.firebase.google.services)
        classpath(libs.firebase.crashlytics.gradle)
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
