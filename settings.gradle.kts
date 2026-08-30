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

rootProject.name = "sahara-safety-companion"

include(":android:app")
include(":android:core:domain")
include(":android:core:data")
include(":android:core:security")
include(":android:core:testing")
include(":android:services:detection")
