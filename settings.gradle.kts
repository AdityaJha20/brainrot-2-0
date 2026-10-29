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

rootProject.name = "brain-rot-2"

// Pure Kotlin domain logic (scoring, export, widget interaction rules). No Android dependency, fast JVM tests.
include(":core")
// Phase 0 Android spike: widget, heatmap bitmap, Room persistence, quick entry.
include(":android")
