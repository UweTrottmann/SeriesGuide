pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.cloud.tools.endpoints-framework-gradle-plugin") {
                useModule("com.google.cloud.tools:endpoints-framework-gradle-plugin:2.1.0")
            }
        }
    }
}

// https://docs.gradle.org/current/userguide/dependency_management.html#sub:centralized-repository-declaration
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For debugdrawer.
        maven { url = uri("https://jitpack.io") }
    }
}

include(":api")
include(":backend")
include(":billing")
include(":widgets")
include(":app")
