import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BeefTech"

// Include Android modules
include(":app")
include(":android:authentication")
include(":android:calf-registration")
include(":android:database")
include(":android:farmer-registration")
include(":android:farm-traceability")
include(":android:management")
include(":android:feed-crib")
include(":android:tag-scanner")
