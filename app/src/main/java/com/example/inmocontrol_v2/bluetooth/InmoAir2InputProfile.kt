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
        hidInputManager.sendMouseScroll(deltaX * SCROLL_SENSITIVITY, deltaY * SCROLL_SENSITIVITY)

    override fun sendLeftClick(): Boolean =
        hidInputManager.sendMouseClick(left = true, right = false, middle = false)

    override fun sendRightClick(): Boolean =
        hidInputManager.sendMouseClick(left = false, right = true, middle = false)

    override fun sendMiddleClick(): Boolean =
        hidInputManager.sendMouseClick(left = false, right = false, middle = true)

    override fun sendDoubleClick(): Boolean {
        val first = hidInputManager.sendMouseClick(left = true, right = false, middle = false)
        if (!first) return false
        Thread.sleep(50)
        val second = hidInputManager.sendMouseClick(left = true, right = false, middle = false)
        return first && second
    }

    override fun sendKey(keyCode: Int, modifiers: Int): Boolean = hidInputManager.sendKey(keyCode, modifiers)
    override fun sendText(text: String): Boolean = hidInputManager.sendText(text)

    override fun sendPlayPause(): Boolean = hidInputManager.playPause()
    override fun sendNextTrack(): Boolean = hidInputManager.nextTrack()
    override fun sendPreviousTrack(): Boolean = hidInputManager.previousTrack()
    override fun sendVolumeUp(): Boolean = hidInputManager.volumeUp()
    override fun sendVolumeDown(): Boolean = hidInputManager.volumeDown()
    override fun sendMute(): Boolean = hidInputManager.mute()

    override fun sendDpadUp(): Boolean = hidInputManager.sendKey(0x52)
    override fun sendDpadDown(): Boolean = hidInputManager.sendKey(0x51)
    override fun sendDpadLeft(): Boolean = hidInputManager.sendKey(0x50)
    override fun sendDpadRight(): Boolean = hidInputManager.sendKey(0x4F)
    override fun sendDpadCenter(): Boolean = hidInputManager.sendKey(0x28)

    override fun sendHidReport(reportId: Int, data: ByteArray): Boolean =
        hidInputManager.sendHidReport(reportId, data)
}
