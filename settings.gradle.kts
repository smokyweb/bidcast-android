

pluginManagement {
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
        maven("https://jitpack.io")
        maven  ("https://a8c-libs.s3.amazonaws.com/android")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("third_party/maven")
        maven("https://jitpack.io")
        maven  ("https://a8c-libs.s3.amazonaws.com/android")
    }
}

rootProject.name = "Bidswipe"
include(":app")
