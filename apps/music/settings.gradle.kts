@file:Suppress("UnstableApiUsage")

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
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
        // MetroSuite toolkits (com.metro.ui / com.metro.system) are AGP 8.x and are consumed
        // as artifacts (this app is AGP 9, so they cannot be includeBuild'd together).
        mavenLocal()
    }
}

rootProject.name = "music"
include(":app")
include(":innertube")
include(":kizzy")
include(":shazamkit")
