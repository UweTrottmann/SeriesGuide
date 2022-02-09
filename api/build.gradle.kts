plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

group = "com.uwetrottmann.seriesguide"
version = "2.2.2-SNAPSHOT"

val sgCompileSdk: Int by rootProject.extra
val sgMinSdk: Int by rootProject.extra
val sgTargetSdk: Int by rootProject.extra

val coreVersion: String by rootProject.extra

android {
    compileSdk = sgCompileSdk
    defaultConfig {
        minSdk = sgMinSdk
        targetSdk = sgTargetSdk
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        // for CI server (reports are not public)
        textReport = true
        textOutput = file("stdout")
    }
}

dependencies {
    implementation("androidx.core:core:$coreVersion")
}

// Note: should build web version with JDK 9+ to get no-frames version.
val androidJavadoc by tasks.registering(Javadoc::class) {
    setSource(android.sourceSets["main"].java.srcDirs)
    title = "SeriesGuide API"

    classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))
    // Add in dependency classpath to avoid cannot find symbol (http://stackoverflow.com/a/34572606/1676363)
    android.libraryVariants.all {
        if (name == "release") {
            this@registering.classpath += javaCompileProvider.get().classpath
        }
    }

    val options = options as StandardJavadocDocletOptions
    options.locale = "en"
    options.windowTitle = "SeriesGuide API"
    options.links("https://developer.android.com/reference/")
    options.linksOffline("https://developer.android.com/reference/", "https://developer.android.com/reference/androidx/")
    options.noDeprecated(true) // Currently nothing is deprecated.
    options.stylesheetFile = file("./javadoc_stylesheet.css")
    val currentJavaVersion = JavaVersion.current()
    if (currentJavaVersion >= JavaVersion.VERSION_1_9) {
        // Specify language level to avoid "The code being documented uses modules" javadoc error
        // if generating with JDK 9+.
        options.addStringOption("-release", "8")
        // Prevent clicking search result adding "undefined" path as not using Java modules.
        options.addBooleanOption("-no-module-directories", true)
    }
}

val androidJavadocJar by tasks.registering(Jar::class) {
    dependsOn(androidJavadoc)
    archiveClassifier.set("javadoc")
    from(androidJavadoc.get().destinationDir)
}

val androidSourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

// Because the components are created only during the afterEvaluate phase, you must
// configure your publications using the afterEvaluate() lifecycle method.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("central") {
                // Applies the component for the release build variant.
                from(components["release"])

                artifact(androidJavadocJar)
                artifact(androidSourcesJar)

                artifactId = "seriesguide-api"

                pom {
                    name.set("SeriesGuide API")
                    description.set("Extension API for SeriesGuide to provide custom actions on media items")
                    url.set("https://seriesgui.de/api")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("uwetrottmann")
                            name.set("Uwe Trottmann")
                        }
                    }

                    scm {
                        connection.set("scm:git:git@github.com:UweTrottmann/SeriesGuide.git")
                        developerConnection.set("scm:git:git@github.com:UweTrottmann/SeriesGuide.git")
                        url.set("https://github.com/UweTrottmann/SeriesGuide.git")
                    }
                }
            }
        }

        // Sonatype Central repository created by gradle-nexus.publish-plugin, see root build.gradle.
    }

    signing {
        if (!rootProject.hasProperty("signing.keyId")
                || !rootProject.hasProperty("signing.password")
                || !rootProject.hasProperty("signing.secretKeyRingFile")) {
            println("WARNING: Signing properties missing, published artifacts will not be signed.")
        }
        sign(publishing.publications["central"])
    }

}
