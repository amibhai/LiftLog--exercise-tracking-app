pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "LiftLog"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":baselineprofile")

include(":core:data")
include(":core:database")
include(":core:domain")
include(":core:testing")
include(":core:ui")

include(":feature:exercises")
include(":feature:history")
include(":feature:home")
include(":feature:onboarding")
include(":feature:progress")
include(":feature:routines")
include(":feature:settings")
include(":feature:widget")
include(":feature:workout")
