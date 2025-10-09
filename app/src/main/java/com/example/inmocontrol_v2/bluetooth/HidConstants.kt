package com.example.inmocontrol_v2.bluetooth

/**
 * HID constants and report descriptors for universal Bluetooth HID compatibility.
 * Based on standard WearMouse descriptor for broad host support.
 */
object HidConstants {

    // Report IDs
    const val ID_KEYBOARD: Byte = 0x01
    const val ID_MOUSE: Byte = 0x02
    const val ID_CONSUMER: Byte = 0x03

    // Timing constants (milliseconds)
    const val PRESS_RELEASE_DELAY_MS = 20L       // Delay between press and release reports
    const val DEBOUNCE_DELAY_MS = 50L            // Minimum interval between repeated actions
    const val KEY_REPEAT_DELAY_MS = 5L           // Delay between characters in text transmission

    // Mouse button masks
    const val MOUSE_BUTTON_LEFT: Byte = 0x01
    const val MOUSE_BUTTON_RIGHT: Byte = 0x02
    const val MOUSE_BUTTON_MIDDLE: Byte = 0x04

    // Keyboard modifier masks
    const val MOD_LEFT_CTRL: Byte = 0x01
    const val MOD_LEFT_SHIFT: Byte = 0x02
    const val MOD_LEFT_ALT: Byte = 0x04
    const val MOD_LEFT_WIN: Byte = 0x08
    const val MOD_RIGHT_CTRL: Byte = 0x10
    const val MOD_RIGHT_SHIFT: Byte = 0x20
    const val MOD_RIGHT_ALT: Byte = 0x40
    const val MOD_RIGHT_WIN: Byte = 0x80.toByte()

    // HID Usage codes for Consumer Control (Media)
    const val USAGE_PLAY_PAUSE: Short = 0x00CD.toShort()
    const val USAGE_NEXT_TRACK: Short = 0x00B5.toShort()
    const val USAGE_PREV_TRACK: Short = 0x00B6.toShort()
    const val USAGE_VOLUME_UP: Short = 0x00E9.toShort()
    const val USAGE_VOLUME_DOWN: Short = 0x00EA.toShort()
    const val USAGE_MUTE: Short = 0x00E2.toShort()

    // Standard HID Report Descriptor (Boot Protocol Compatible)
    // Matches WearMouse specification for universal compatibility
    val REPORT_DESCRIPTOR = byteArrayOf(
        // Keyboard Report (Report ID 1)
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),        // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), ID_KEYBOARD,          //   Report ID (1)
        0x05.toByte(), 0x07.toByte(),        //   Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(),        //   Usage Minimum (224)
        0x29.toByte(), 0xE7.toByte(),        //   Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),        //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),        //   Input (Data, Variable, Absolute) - Modifier byte
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x81.toByte(), 0x01.toByte(),        //   Input (Constant) - Reserved byte
        0x95.toByte(), 0x06.toByte(),        //   Report Count (6)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(),        //   Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(),        //   Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(),        //   Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(),        //   Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(),        //   Input (Data, Array) - Key array
        0xC0.toByte(),                       // End Collection

        // Mouse Report (Report ID 2)
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),        // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), ID_MOUSE,             //   Report ID (2)
        0x09.toByte(), 0x01.toByte(),        //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(),        //   Collection (Physical)
        0x05.toByte(), 0x09.toByte(),        //     Usage Page (Buttons)
        0x19.toByte(), 0x01.toByte(),        //     Usage Minimum (1)
        0x29.toByte(), 0x03.toByte(),        //     Usage Maximum (3)
        0x15.toByte(), 0x00.toByte(),        //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //     Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(),        //     Report Count (3)
        0x75.toByte(), 0x01.toByte(),        //     Report Size (1)
        0x81.toByte(), 0x02.toByte(),        //     Input (Data, Variable, Absolute) - Buttons
        0x95.toByte(), 0x01.toByte(),        //     Report Count (1)
        0x75.toByte(), 0x05.toByte(),        //     Report Size (5)
        0x81.toByte(), 0x01.toByte(),        //     Input (Constant) - Padding
        0x05.toByte(), 0x01.toByte(),        //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),        //     Usage (X)
        0x09.toByte(), 0x31.toByte(),        //     Usage (Y)
        0x09.toByte(), 0x38.toByte(),        //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x03.toByte(),        //     Report Count (3)
        0x81.toByte(), 0x06.toByte(),        //     Input (Data, Variable, Relative) - X, Y, Wheel
        0xC0.toByte(),                       //   End Collection (Physical)
        0xC0.toByte(),                       // End Collection (Application)

        // Consumer Control Report (Report ID 3)
        0x05.toByte(), 0x0C.toByte(),        // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(),        // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), ID_CONSUMER,          //   Report ID (3)
        0x19.toByte(), 0x00.toByte(),        //   Usage Minimum (0)
        0x2A.toByte(), 0x3C.toByte(), 0x02.toByte(), // Usage Maximum (572)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x26.toByte(), 0x3C.toByte(), 0x02.toByte(), // Logical Maximum (572)
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x75.toByte(), 0x10.toByte(),        //   Report Size (16)
        0x81.toByte(), 0x00.toByte(),        //   Input (Data, Array)
        0xC0.toByte()                        // End Collection
    )
}

