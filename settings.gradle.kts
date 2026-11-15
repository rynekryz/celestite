pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Celestite"
include(":app")
include(":core:model")
include(":core:datastore")
include(":core:designsystem")
include(":feature:recorder")
include(":feature:gallery")
include(":feature:settings")
include(":service")
