pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "beeftech"





// Include Android modules
include(":android:app")
include(":android:authentication")
include(":android:calf-registration")
include(":android:database")
include(":android:farmer-registration")
include(":android:farm-traceability")
include(":android:feed-crib")

// Include Backend modules
include(":backend:api")
include(":backend:authentication")
include(":backend:sync")