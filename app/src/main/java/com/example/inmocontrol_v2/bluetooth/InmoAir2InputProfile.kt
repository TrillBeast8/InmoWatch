package com.example.inmocontrol_v2.bluetooth

/**
 * Optimized InmoAir2 input profile with device-specific optimizations and direct delegation
 */
class InmoAir2InputProfile(private val hidInputManager: HidInputManager) : InputProfile {

    companion object {
        // InmoAir2-specific sensitivity multipliers
        private const val MOUSE_SENSITIVITY = 1.2f
        private const val SCROLL_SENSITIVITY = 0.8f
    }

    // Optimized mouse methods with device-specific scaling
    override fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean =
        hidInputManager.sendMouseMovement(deltaX * MOUSE_SENSITIVITY, deltaY * MOUSE_SENSITIVITY)

    override fun sendScroll(deltaX: Float, deltaY: Float): Boolean =
        hidInputManager.sendScroll(deltaX * SCROLL_SENSITIVITY, deltaY * SCROLL_SENSITIVITY)

    // Direct delegation for all other methods
    override fun sendLeftClick(): Boolean = hidInputManager.sendLeftClick()
    override fun sendRightClick(): Boolean = hidInputManager.sendRightClick()
    override fun sendMiddleClick(): Boolean = hidInputManager.sendMiddleClick()
    override fun sendDoubleClick(): Boolean = hidInputManager.sendDoubleClick()

    override fun sendKey(keyCode: Int, modifiers: Int): Boolean = hidInputManager.sendKey(keyCode, modifiers)
    override fun sendText(text: String): Boolean = hidInputManager.sendText(text)

    override fun sendPlayPause(): Boolean = hidInputManager.sendPlayPause()
    override fun sendNextTrack(): Boolean = hidInputManager.sendNextTrack()
    override fun sendPreviousTrack(): Boolean = hidInputManager.sendPreviousTrack()
    override fun sendVolumeUp(): Boolean = hidInputManager.sendVolumeUp()
    override fun sendVolumeDown(): Boolean = hidInputManager.sendVolumeDown()
    override fun sendMute(): Boolean = hidInputManager.sendMute()

    override fun sendDpadUp(): Boolean = hidInputManager.sendDpadUp()
    override fun sendDpadDown(): Boolean = hidInputManager.sendDpadDown()
    override fun sendDpadLeft(): Boolean = hidInputManager.sendDpadLeft()
    override fun sendDpadRight(): Boolean = hidInputManager.sendDpadRight()
    override fun sendDpadCenter(): Boolean = hidInputManager.sendDpadCenter()

    override fun sendHidReport(reportId: Int, data: ByteArray): Boolean =
        hidInputManager.sendHidReport(reportId, data)
}
