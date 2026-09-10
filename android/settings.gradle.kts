pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BeefTechAndroid"

include(":app", ":database", ":authentication", ":calf-registration", ":farmer-registration", ":farm-traceability", ":feed-crib")
