# Add project specific ProGuard rules here.
# Optimized for Samsung Watch - small footprint, fast performance

# Keep application class
-keep public class com.example.inmocontrol_v2.MainActivity { *; }

# Keep Bluetooth and HID related classes - critical for functionality
-keep class * extends android.bluetooth.** { *; }
-keep class androidx.bluetooth.** { *; }
-keep class com.example.inmocontrol_v2.bluetooth.** { *; }
-keep class com.example.inmocontrol_v2.hid.** { *; }

# Keep sensor fusion classes for performance
-keep class com.example.inmocontrol_v2.sensors.** { *; }

# Keep data classes and stores
-keep class com.example.inmocontrol_v2.data.** { *; }

# Compose optimizations - keep only what's needed
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.wear.compose.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Fix Compose lock verification warnings - optimize snapshot state list
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep Compose State classes to prevent lock verification issues
-keep class androidx.compose.runtime.snapshots.** { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.** { *; }

# Additional fix: Force inline optimization for Compose runtime
-assumenosideeffects class androidx.compose.runtime.snapshots.SnapshotStateList {
    public *** conditionalUpdate*(...);
    public *** update(...);
    public *** mutate(...);
}

# Aggressive obfuscation - remove unused code
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coroutines optimization - keep volatile fields
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep service classes - required for HID functionality
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends androidx.wear.tiles.TileService { *; }

# Remove ALL logging in release builds - significant size reduction
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Optimize enums and strings
-optimizations !code/simplification/enum,!code/allocation/variable
-allowaccessmodification
-repackageclasses ''

# Aggressive optimization passes for smaller APK
-optimizationpasses 5

# Remove unused resources
-keepclassmembers class **.R$* {
    public static <fields>;
}

# DataStore optimization
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Kotlin metadata optimization
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Remove assertions for smaller code
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}
