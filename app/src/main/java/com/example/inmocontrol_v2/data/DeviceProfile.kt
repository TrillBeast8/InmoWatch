package com.example.inmocontrol_v2.data

/**
 * Optimized Device profile enumeration - reduced memory footprint
 */
enum class DeviceProfile(val displayName: String) {
    /**
     * Mouse mode - for cursor movement and clicking
     */
    Mouse("Mouse"),

    /**
     * Keyboard mode - for text input and hotkeys
     */
    Keyboard("Keyboard"),

    /**
     * Media mode - for media controls (play, pause, volume)
     */
    Media("Media Controls"),

    /**
     * Gamepad mode - for D-pad controls
     */
    Gamepad("D-Pad"),

    /**
     * Touchpad mode - for touch-based input
     */
    Touchpad("Touchpad"),

    /**
     * Generic HID profile - fallback for unknown devices
     */
    Generic("Generic HID"),

    /**
     * InmoAir2 specific profile - optimized for InmoAir glasses
     */
    InmoAir2("InmoAir2"),

    /**
     * Universal profile - for devices that don't fit other categories
     */
    Universal("Universal Device")
}
