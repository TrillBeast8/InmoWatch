package com.example.inmocontrol_v2.bluetooth

/**
 * Optimized Generic input profile with direct delegation to HidInputManager
 */
class GenericInputProfile(private val hidInputManager: HidInputManager) : InputProfile {

    // Direct delegation for better performance - no redundant method calls
    override fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean =
        hidInputManager.sendMouseMovement(deltaX, deltaY)

    override fun sendLeftClick(): Boolean = hidInputManager.sendLeftClick()
    override fun sendRightClick(): Boolean = hidInputManager.sendRightClick()
    override fun sendMiddleClick(): Boolean = hidInputManager.sendMiddleClick()
    override fun sendDoubleClick(): Boolean = hidInputManager.sendDoubleClick()
    override fun sendScroll(deltaX: Float, deltaY: Float): Boolean =
        hidInputManager.sendScroll(deltaX, deltaY)

    override fun sendKey(keyCode: Int, modifiers: Int): Boolean =
        hidInputManager.sendKey(keyCode, modifiers)
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
