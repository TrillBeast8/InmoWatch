plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.inmocontrol_v2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.inmocontrol_v2"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Optimize for performance and size
        vectorDrawables.useSupportLibrary = true
        resourceConfigurations += listOf("en")
    }

    buildFeatures {
        compose = true
        buildConfig = false  // Disable BuildConfig generation to reduce APK size
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true  // Remove unused resources
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Additional optimizations
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
    }

    // Optimize compilation
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = false  // Disable if not needed
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }

    // Optimize packaging
    packagingOptions {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/license.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/notice.txt",
                "/META-INF/ASL2.0",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin"
            )
        }
    }
}

dependencies {
    // INMO AR SDK
    implementation("com.inmo:inmo_arsdk:0.0.1")

    // Core dependencies - optimized versions
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")  // Combined activity support

    // Compose BOM for version alignment - optimized
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))  // Latest stable
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")

    // Material Design - add Material3 for compatibility
    implementation("androidx.compose.material:material")  // For Wear OS
    implementation("androidx.compose.material3:material3") // For Material3 components

    // Icons - add extended icons for missing icons
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended") // Needed for Bluetooth icons

    // Debug tools - only in debug builds
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation - lightweight
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // DataStore - keep for settings
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines - essential
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Wear Compose - optimized selection
    implementation("androidx.wear.compose:compose-foundation:1.2.1")
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation("androidx.wear.compose:compose-navigation:1.2.1")
    debugImplementation("androidx.wear.compose:compose-ui-tooling:1.2.1")

    // Wear OS Tiles - add missing concurrent futures
    implementation("androidx.wear.tiles:tiles:1.1.0")
    implementation("androidx.wear.tiles:tiles-material:1.1.0")
    implementation("androidx.wear.protolayout:protolayout:1.1.0")
    implementation("androidx.concurrent:concurrent-futures:1.1.0") // Needed for CallbackToFutureAdapter

    // Lifecycle for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
}
