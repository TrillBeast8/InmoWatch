// ...existing code...
package com.example.inmocontrol_v2.hid

// Backwards-compatibility alias: keep the hid package type available for any code
// that still imports com.example.inmocontrol_v2.hid.InputProfile. Implementations live
// in com.example.inmocontrol_v2.bluetooth (GenericInputProfile, InmoAir2InputProfile).
@Suppress("DEPRECATION")
@Deprecated("Use com.example.inmocontrol_v2.bluetooth.InputProfile instead")
typealias InputProfile = com.example.inmocontrol_v2.bluetooth.InputProfile

