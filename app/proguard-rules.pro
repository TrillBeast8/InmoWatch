# Optimization flags for better performance and smaller APK size
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove debug information to reduce size
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Keep essential Android classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Optimize Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Optimize Compose - keep only essential classes
-keep class androidx.compose.** { *; }
-keep class androidx.wear.compose.** { *; }
-dontwarn androidx.compose.**

# Optimize Bluetooth classes - essential for HID functionality
-keep class android.bluetooth.** { *; }
-keep class com.example.inmocontrol_v2.bluetooth.** { *; }
-keep class com.example.inmocontrol_v2.hid.** { *; }

# Remove unused resources and code
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Optimize data classes and enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep data classes for serialization
-keep @kotlinx.serialization.Serializable class * { *; }

# Navigation optimizations
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.Navigator

# Wear OS specific optimizations
-keep class androidx.wear.** { *; }
-dontwarn androidx.wear.**

# Remove unused DataStore internals
-keep class androidx.datastore.** { *; }
-keep interface androidx.datastore.** { *; }

# INMO SDK optimizations
-keep class com.inmo.** { *; }
-dontwarn com.inmo.**

# Coroutines optimization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Remove reflective access warnings
-dontwarn java.lang.invoke.StringConcatFactory
