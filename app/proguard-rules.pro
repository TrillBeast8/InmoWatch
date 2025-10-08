# Add project specific ProGuard rules here.
# Updated for Android 2025 compatibility

# Keep application class
-keep public class com.example.inmocontrol_v2.MainActivity { *; }

# Keep Bluetooth and HID related classes - updated for 2025 APIs
-keep class * extends android.bluetooth.** { *; }
-keep class androidx.bluetooth.** { *; }
-keep class com.example.inmocontrol_v2.bluetooth.** { *; }
-keep class com.example.inmocontrol_v2.hid.** { *; }

# Keep sensor fusion classes for performance
-keep class com.example.inmocontrol_v2.sensors.** { *; }

# Keep data classes and stores
-keep class com.example.inmocontrol_v2.data.** { *; }

# Compose optimizations for 2025
-keep class androidx.compose.** { *; }
-keep class androidx.wear.compose.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Coroutines optimization
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep service classes
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends androidx.wear.tiles.TileService { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Optimize enums
-optimizations !code/simplification/enum

# Aggressive optimizations for smaller size
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# Keep line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable

# Fix for common R8 warnings in 2025
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# Fix for Guava warnings
-dontwarn com.google.common.base.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Wear OS specific rules
-keep class androidx.wear.** { *; }
-keep class com.google.android.wearable.** { *; }
