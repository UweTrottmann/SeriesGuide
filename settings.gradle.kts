pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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
include(":billing")
include(":widgets")
include(":app")
