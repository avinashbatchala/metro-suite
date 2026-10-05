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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MetroSuite shared design system (com.metro.ui:metro-ui-android). The toolkit is
        // published from metro-os/toolkits; see scripts/build-apks.sh (maven-local prebuild).
        // Its AGP (8.7.3) differs from this app's, so it is consumed as an artifact rather
        // than includeBuild (Gradle forbids mixed AGP versions in one composite build).
        mavenLocal()
    }
}

rootProject.name = "MetroWeather"
include(":app")
