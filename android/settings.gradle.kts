pluginManagement {
    repositories {
        google()
        // Mirror Maven Central to avoid transient HTTP 429 responses in CI runners.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Mirror Maven Central to avoid transient HTTP 429 responses in CI runners.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "LandsOfJailAssistant"
include(":app")
