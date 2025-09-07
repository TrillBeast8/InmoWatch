package com.example.inmocontrol_v2.bluetooth

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

/**
 * HID Constants matching proven WearMouse implementation for reliable connectivity
 * Using exact same Report IDs and descriptor structure as wearmouse reference
 */
object HidConstants {

    // Report IDs - EXACT match with wearmouse for compatibility
    const val ID_KEYBOARD: Byte = 1
    const val ID_MOUSE: Byte = 2
    const val ID_CONSUMER: Byte = 3  // Our addition for media controls

    // Mouse button masks
    const val MOUSE_BUTTON_LEFT = 0x01
    const val MOUSE_BUTTON_RIGHT = 0x02
    const val MOUSE_BUTTON_MIDDLE = 0x04

    // Consumer Control Usage IDs for media functions
    const val USAGE_PLAY_PAUSE = 0xCD     // Play/Pause
    const val USAGE_NEXT_TRACK = 0xB5     // Next Track
    const val USAGE_PREV_TRACK = 0xB6     // Previous Track
    const val USAGE_VOLUME_UP = 0xE9      // Volume Up
    const val USAGE_VOLUME_DOWN = 0xEA    // Volume Down
    const val USAGE_MUTE = 0xE2           // Mute

    /**
     * HID Descriptor based on proven WearMouse pattern with our media controls addition
     * This ensures maximum compatibility with HID hosts
     */
    private val HIDD_REPORT_DESC = byteArrayOf(
        // Keyboard (ID=1) - Exact wearmouse structure
        0x05.toByte(), 0x01, // Usage page (Generic Desktop)
        0x09.toByte(), 0x06, // Usage (Keyboard)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), ID_KEYBOARD, //    Report ID
        0x05.toByte(), 0x07, //       Usage page (Key Codes)
        0x19.toByte(), 0xE0.toByte(), //       Usage minimum (224)
        0x29.toByte(), 0xE7.toByte(), //       Usage maximum (231)
        0x15.toByte(), 0x00, //       Logical minimum (0)
        0x25.toByte(), 0x01, //       Logical maximum (1)
        0x75.toByte(), 0x01, //       Report size (1)
        0x95.toByte(), 0x08, //       Report count (8)
        0x81.toByte(), 0x02, //       Input (Data, Variable, Absolute) ; Modifier byte
        0x75.toByte(), 0x08, //       Report size (8)
        0x95.toByte(), 0x01, //       Report count (1)
        0x81.toByte(), 0x01, //       Input (Constant)                 ; Reserved byte
        0x75.toByte(), 0x08, //       Report size (8)
        0x95.toByte(), 0x06, //       Report count (6)
        0x15.toByte(), 0x00, //       Logical Minimum (0)
        0x25.toByte(), 0x65, //       Logical Maximum (101)
        0x05.toByte(), 0x07, //       Usage page (Key Codes)
        0x19.toByte(), 0x00, //       Usage Minimum (0)
        0x29.toByte(), 0x65, //       Usage Maximum (101)
        0x81.toByte(), 0x00, //       Input (Data, Array)              ; Key array (6 keys)
        0xC0.toByte(),              // End Collection

        // Mouse (ID=2) - Exact wearmouse structure
        0x05.toByte(), 0x01, // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02, // Usage (Mouse)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), ID_MOUSE,    //    Report ID
        0x09.toByte(), 0x01, //    Usage (Pointer)
        0xA1.toByte(), 0x00, //    Collection (Physical)
        0x05.toByte(), 0x09, //       Usage Page (Buttons)
        0x19.toByte(), 0x01, //       Usage minimum (1)
        0x29.toByte(), 0x03, //       Usage maximum (3)
        0x15.toByte(), 0x00, //       Logical minimum (0)
        0x25.toByte(), 0x01, //       Logical maximum (1)
        0x75.toByte(), 0x01, //       Report size (1)
        0x95.toByte(), 0x03, //       Report count (3)
        0x81.toByte(), 0x02, //       Input (Data, Variable, Absolute)
        0x75.toByte(), 0x05, //       Report size (5)
        0x95.toByte(), 0x01, //       Report count (1)
        0x81.toByte(), 0x01, //       Input (constant)                 ; 5 bit padding
        0x05.toByte(), 0x01, //       Usage page (Generic Desktop)
        0x09.toByte(), 0x30, //       Usage (X)
        0x09.toByte(), 0x31, //       Usage (Y)
        0x09.toByte(), 0x38, //       Usage (Wheel)
        0x15.toByte(), 0x81.toByte(), //       Logical minimum (-127)
        0x25.toByte(), 0x7F, //       Logical maximum (127)
        0x75.toByte(), 0x08, //       Report size (8)
        0x95.toByte(), 0x03, //       Report count (3)
        0x81.toByte(), 0x06, //       Input (Data, Variable, Relative)
        0xC0.toByte(),              //    End Collection
        0xC0.toByte(),              // End Collection

        // Consumer Control (ID=3) - Our addition for media controls
        0x05.toByte(), 0x0C, // Usage page (Consumer)
        0x09.toByte(), 0x01, // Usage (Consumer Control)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), ID_CONSUMER, //    Report ID
        0x19.toByte(), 0x00, //    Usage Minimum
        0x2A.toByte(), 0x3C.toByte(), 0x02, // Usage Maximum (0x23C)
        0x15.toByte(), 0x00, //    Logical Minimum (0)
        0x26.toByte(), 0x3C.toByte(), 0x02, // Logical Maximum (0x23C)
        0x95.toByte(), 0x01, //    Report Count (1)
        0x75.toByte(), 0x10, //    Report Size (16)
        0x81.toByte(), 0x00, //    Input (Data,Array,Abs)
        0xC0.toByte()              // End Collection
    )

    // Public accessor for other code (GattHidServer) that needs the raw descriptor bytes
    val REPORT_DESCRIPTOR: ByteArray get() = HIDD_REPORT_DESC

    /**
     * SDP Settings for HID Device registration - based on proven WearMouse configuration
     * These settings are critical for successful HID registration
     */
    val SDP_SETTINGS: BluetoothHidDeviceAppSdpSettings by lazy {
        BluetoothHidDeviceAppSdpSettings(
            "InmoWatch Remote",        // Device name
            "InmoWatch HID Remote Control", // Device description
            "InmoLabs",               // Provider name
            BluetoothHidDevice.SUBCLASS1_COMBO,  // Device subclass (combo device)
            HIDD_REPORT_DESC          // HID descriptor
        )
    }

    /**
     * QoS Settings for outgoing data - optimized for responsive input
     * Based on WearMouse proven configuration for minimal latency
     */
    val QOS_OUT: BluetoothHidDeviceAppQosSettings by lazy {
        BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,  // Service type
            800,    // Token rate (bytes/second) - high for responsive input
            9,      // Token bucket size - small for low latency
            800,    // Peak bandwidth - match token rate
            0,      // Latency (microseconds) - 0 for best effort
            0xFFFFFF  // Delay variation - large value for best effort (24-bit max)
        )
    }
}
