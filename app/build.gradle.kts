plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.inmocontrol_v2"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.inmocontrol_v2"
        minSdk = 28
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    // INMO AR SDK
    implementation("com.inmo:inmo_arsdk:0.0.1")

    // AndroidX dependencies compatible with compileSdk 33 and AGP 8.2.2
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.emoji2:emoji2:1.3.0")
    implementation("androidx.annotation:annotation-experimental:1.3.1")
    implementation("androidx.core:core-viewtree:1.0.0")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    // Compose dependencies compatible with Kotlin 1.9.22 and compileSdk 33
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
    implementation("androidx.compose.material:material:1.5.3")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.foundation:foundation:1.5.3")
    implementation("androidx.compose.runtime:runtime:1.5.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Wear OS Tile dependencies compatible with compileSdk 33
    implementation("androidx.wear.tiles:tiles:1.1.0")
    implementation("androidx.wear.tiles:tiles-material:1.1.0")
    implementation("androidx.wear.protolayout:protolayout:1.1.0")
    implementation("androidx.wear.protolayout:protolayout-expression:1.1.0")
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("com.google.guava:guava:31.1-android")
}