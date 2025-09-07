package com.example.inmocontrol_v2.data

/**
 * Simple device profile enum for different input modes
 * This represents the type of HID device we're emulating
 */
enum class DeviceProfile {
    /**
     * Mouse mode - for cursor movement and clicking
     */
    Mouse,

    /**
     * Keyboard mode - for text input and hotkeys
     */
    Keyboard,

    /**
     * Media mode - for media controls (play, pause, volume)
     */
    Media,

    /**
     * Gamepad mode - for D-pad controls
     */
    Gamepad,

    /**
     * InmoAir2 specific profile - optimized for InmoAir glasses
     */
    InmoAir2,

    /**
     * Generic HID profile - fallback for unknown devices
     */
    GenericHid;

    /**
     * Get user-friendly display name
     */
    val displayName: String
        get() = when (this) {
            Mouse -> "Mouse"
            Keyboard -> "Keyboard"
            Media -> "Media Controls"
            Gamepad -> "D-Pad"
            InmoAir2 -> "InmoAir2"
            GenericHid -> "Generic HID"
        }

    /**
     * Check if this profile supports mouse-like operations
     */
    val supportsMouseInput: Boolean
        get() = this == Mouse || this == InmoAir2 || this == GenericHid

    /**
     * Check if this profile supports keyboard input
     */
    val supportsKeyboardInput: Boolean
        get() = this == Keyboard || this == InmoAir2 || this == GenericHid
}
