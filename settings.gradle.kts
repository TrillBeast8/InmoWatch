pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        id("com.android.application") version "8.2.2"
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("androidx.compose.compiler") version "1.5.8"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS);
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        //add inmosdk repositories
        maven { url = uri("https://gitee.com/inmolens/inmo-ar-sdk/raw/master") }
    }
}
rootProject.name = "InmoControl"
include(":app")
