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

android {
    namespace = "com.battlelancer.seriesguide.api"
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
        // Note: do not use textOutput = file("stdout"), just set no file.
    }

    publishing {
        singleVariant("release") {
            // The API currently does not allow customizing the generation task (e.g. title, custom
            // CSS). See Git history to restore the custom javadoc_stylesheet.css.
            withJavadocJar()
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
}

publishing {
    publications {
        create<MavenPublication>("central") {
            // Because the components are created only during the afterEvaluate phase, you must
            // configure your publications using the afterEvaluate() lifecycle method.
            afterEvaluate {
                // Applies the component for the release variant created in android block.
                from(components["release"])
            }

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
    // https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent
    useGpgCmd()
    sign(publishing.publications["central"])
}
