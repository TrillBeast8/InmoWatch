package com.example.inmocontrol_v2.hid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.inmocontrol_v2.bluetooth.HidDeviceApp
import com.example.inmocontrol_v2.bluetooth.HidDeviceProfile
import com.example.inmocontrol_v2.bluetooth.HidInputManager
import kotlinx.coroutines.*

/**
 * Core HID orchestrator - Manages Bluetooth HID profile registration, device connections,
 * and input dispatching.
 *
 * Critical Rules from Copilot Instructions:
 * - Register/unregister HID profile on main thread only
 * - Release proxy resources to prevent memory leaks
 * - Maintain foreground notification while paired (Samsung doze prevention)
 * - Retry registerApp() with exponential backoff on STATE_DISCONNECTING
 */
class HidService : Service(), HidServiceApi {

    companion object {
        private const val TAG = "HidService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hid_service_channel"
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var hidDeviceProfile: HidDeviceProfile
    private lateinit var hidDeviceApp: HidDeviceApp
    private lateinit var hidInputManager: HidInputManager

    private var connectedDevice: BluetoothDevice? = null
    private var isServiceReady = false
    private var isProfileConnected = false
    private var isAppRegistered = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initializeBluetoothComponents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun initializeBluetoothComponents() {
        serviceScope.launch {
            try {
                val bluetoothManager = getSystemService(BluetoothManager::class.java)
                bluetoothAdapter = bluetoothManager.adapter

                hidDeviceProfile = HidDeviceProfile()
                hidDeviceApp = HidDeviceApp()
                hidInputManager = HidInputManager(hidDeviceProfile)

                hidDeviceApp.setDeviceStateListener(deviceStateListener)
                hidDeviceProfile.registerServiceListener(this@HidService, serviceStateListener)
                hidDeviceProfile.registerDeviceListener(profileDeviceStateListener)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Bluetooth components", e)
                HidClient.setConnectionError("Bluetooth init failed")
            }
        }
    }

    // Device state listener
    private val deviceStateListener = object : HidDeviceApp.DeviceStateListener {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            serviceScope.launch {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedDevice = device
                        hidInputManager.setConnectedDevice(device)
                        HidClient.setConnectionState(true)
                        HidClient.setConnectionError(null)
                        try {
                            Log.d(TAG, "Device connected: ${device.name ?: device.address}")
                        } catch (_: SecurityException) {
                            Log.d(TAG, "Device connected: ${device.address}")
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedDevice = null
                        hidInputManager.setConnectedDevice(null)
                        HidClient.setConnectionState(false)
                        try {
                            Log.d(TAG, "Device disconnected: ${device.name ?: device.address}")
                        } catch (_: SecurityException) {
                            Log.d(TAG, "Device disconnected: ${device.address}")
                        }
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        try {
                            Log.d(TAG, "Device connecting: ${device.name ?: device.address}")
                        } catch (_: SecurityException) {
                            Log.d(TAG, "Device connecting: ${device.address}")
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        try {
                            Log.d(TAG, "Device disconnecting: ${device.name ?: device.address}")
                        } catch (_: SecurityException) {
                            Log.d(TAG, "Device disconnecting: ${device.address}")
                        }
                        // DO NOT call retryRegisterApp() here - this is a device state change,
                        // not a profile service disconnection. Device disconnecting is normal
                        // during connection handshake and should not trigger re-registration.
                    }
                }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            isAppRegistered = registered
            updateServiceReadyState()
            Log.d(TAG, "App registration status: $registered")
        }
    }

    // Service state listener
    private val serviceStateListener = object : HidDeviceProfile.ServiceStateListener {
        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            Log.d(TAG, "HID profile service state changed: ${proxy != null}")

            isProfileConnected = proxy != null

            if (proxy != null) {
                // Profile connected - register app and WAIT for confirmation
                Log.d(TAG, "Profile connected, starting app registration...")
                serviceScope.launch {
                    delay(100) // Small delay for stability
                    hidDeviceProfile.registerApp(hidDeviceApp)
                }
                // DO NOT set isServiceReady here - wait for onAppStatusChanged callback
            } else {
                // Profile disconnected - reset all flags
                isProfileConnected = false
                isAppRegistered = false
                Log.d(TAG, "Profile disconnected - resetting service state")
            }

            updateServiceReadyState()
        }
    }

    // Profile device state listener
    private val profileDeviceStateListener = object : HidDeviceProfile.DeviceStateListener {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Profile device state: $state")
            }
        }

        override fun onAppStatusChanged(device: BluetoothDevice, registered: Boolean) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Profile app status: $registered")
            }
        }
    }

    /**
     * Update service ready state based on profile connection AND app registration.
     * Service is only ready when BOTH conditions are met.
     */
    private fun updateServiceReadyState() {
        val wasReady = isServiceReady

        // CRITICAL: Service is only ready when BOTH conditions are met
        isServiceReady = isProfileConnected && isAppRegistered

        if (isServiceReady != wasReady) {
            Log.d(TAG, "Service ready state changed: $isServiceReady (profile=$isProfileConnected, app=$isAppRegistered)")
            HidClient.setServiceReady(isServiceReady)
            HidClient.setHidProfileAvailable(isServiceReady)

            // If we just became ready, notify HidClient immediately
            if (isServiceReady) {
                HidClient.clearError()
                Log.d(TAG, "✅ HID Service fully initialized and ready for connections")
            }
        }
    }

    /**
     * Retry registerApp() with exponential backoff (100ms → 500ms → 1s)
     */
    private suspend fun retryRegisterApp() {
        val delays = listOf(100L, 500L, 1000L)
        for (delay in delays) {
            delay(delay)
            if (hidDeviceProfile.registerApp(hidDeviceApp)) {
                Log.d(TAG, "Successfully re-registered HID app after ${delay}ms")
                break
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "HID Service",
            NotificationManager.IMPORTANCE_HIGH  // Changed from LOW to HIGH to prevent app suspension
        ).apply {
            description = "Keeps Bluetooth HID connection active"
            setShowBadge(false)  // Don't show notification badge
            enableVibration(false)  // Don't vibrate
            enableLights(false)  // Don't use LED
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InmoWatch HID Active")
            .setContentText("Bluetooth controller ready")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // High priority keeps app alive
            .setCategory(NotificationCompat.CATEGORY_SERVICE)  // Mark as service
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)  // Immediate start
            .build()
    }

    private fun cleanup() {
        serviceScope.cancel()
        hidInputManager.cleanup()
        hidDeviceProfile.unregisterApp()
        hidDeviceProfile.cleanup(bluetoothAdapter)
        connectedDevice = null
    }

    // ==================== HidServiceApi Implementation ====================

    override fun isReady(): Boolean = isServiceReady && isAppRegistered

    override fun isDeviceConnected(): Boolean = connectedDevice != null

    override fun connectToDevice(device: BluetoothDevice): Boolean {
        // CRITICAL: Strict ready check before attempting connection
        if (!isServiceReady) {
            val reason = when {
                !isProfileConnected -> "HID profile not connected"
                !isAppRegistered -> "HID app not registered"
                else -> "Service not initialized"
            }
            Log.e(TAG, "❌ Cannot connect to ${device.address}: $reason")
            HidClient.setConnectionError("Service not ready: $reason")
            return false
        }

        if (!isProfileConnected || !isAppRegistered) {
            Log.w(TAG, "Service not ready or app not registered; cannot connect")
            HidClient.setConnectionError("Service not fully initialized")
            return false
        }
        
        Log.d(TAG, "✅ Connecting to device: ${device.address}")
        val success = hidDeviceProfile.connect(device)
        if (!success) {
            HidClient.setConnectionError("Failed to initiate connection to ${device.address}")
            Log.e(TAG, "❌ Connection initiation failed")
        }
        return success
    }

    // Mouse operations
    override fun sendMouseMovement(deltaX: Float, deltaY: Float): Boolean {
        return hidInputManager.sendMouseMovement(deltaX, deltaY)
    }

    override fun sendLeftClick(): Boolean {
        return hidInputManager.sendMouseClick(left = true, right = false, middle = false)
    }

    override fun sendRightClick(): Boolean {
        return hidInputManager.sendMouseClick(left = false, right = true, middle = false)
    }

    override fun sendMiddleClick(): Boolean {
        return hidInputManager.sendMouseClick(left = false, right = false, middle = true)
    }

    override fun sendDoubleClick(): Boolean {
        val success1 = sendLeftClick()
        Thread.sleep(50) // Small delay between clicks
        val success2 = sendLeftClick()
        return success1 && success2
    }

    override fun sendMouseScroll(deltaX: Float, deltaY: Float): Boolean {
        return hidInputManager.sendMouseScroll(deltaX, deltaY)
    }

    // Keyboard operations
    override fun sendKey(keyCode: Int, modifiers: Int): Boolean {
        return hidInputManager.sendKey(keyCode, modifiers)
    }

    override fun sendText(text: String): Boolean {
        return hidInputManager.sendText(text)
    }

    // Media controls
    override fun playPause(): Boolean {
        return hidInputManager.playPause()
    }

    override fun nextTrack(): Boolean {
        return hidInputManager.nextTrack()
    }

    override fun previousTrack(): Boolean {
        return hidInputManager.previousTrack()
    }

    override fun volumeUp(): Boolean {
        return hidInputManager.volumeUp()
    }

    override fun volumeDown(): Boolean {
        return hidInputManager.volumeDown()
    }

    override fun mute(): Boolean {
        return hidInputManager.mute()
    }

    // D-Pad navigation
    override fun dpad(direction: Int): Boolean {
        // Map direction to appropriate HID command
        return when (direction) {
            0 -> sendDpadUp()
            1 -> sendDpadDown()
            2 -> sendDpadLeft()
            3 -> sendDpadRight()
            4 -> sendDpadCenter()
            // Diagonals: Send two keys simultaneously
            5 -> sendDpadDownLeft()     // ↙
            6 -> sendDpadDownRight()    // ↘
            7 -> sendDpadUpLeft()       // ↖
            8 -> sendDpadUpRight()      // ↗
            else -> false
        }
    }

    override fun sendDpadUp(): Boolean {
        return hidInputManager.sendKey(0x52) // UP_ARROW
    }

    override fun sendDpadDown(): Boolean {
        return hidInputManager.sendKey(0x51) // DOWN_ARROW
    }

    override fun sendDpadLeft(): Boolean {
        return hidInputManager.sendKey(0x50) // LEFT_ARROW
    }

    override fun sendDpadRight(): Boolean {
        return hidInputManager.sendKey(0x4F) // RIGHT_ARROW
    }

    override fun sendDpadCenter(): Boolean {
        return hidInputManager.sendKey(0x28) // ENTER
    }

    // Diagonal movements - send two keys simultaneously
    private fun sendDpadUpLeft(): Boolean {
        return hidInputManager.sendKeys(intArrayOf(0x52, 0x50)) // UP + LEFT
    }

    private fun sendDpadUpRight(): Boolean {
        return hidInputManager.sendKeys(intArrayOf(0x52, 0x4F)) // UP + RIGHT
    }

    private fun sendDpadDownLeft(): Boolean {
        return hidInputManager.sendKeys(intArrayOf(0x51, 0x50)) // DOWN + LEFT
    }

    private fun sendDpadDownRight(): Boolean {
        return hidInputManager.sendKeys(intArrayOf(0x51, 0x4F)) // DOWN + RIGHT
    }

    // ESC key for back functionality
    override fun sendEscape(): Boolean {
        return hidInputManager.sendKey(0x29) // ESC
    }

    // Raw HID
    override fun sendRawInput(data: ByteArray): Boolean {
        // Not implemented - use specific methods instead
        Log.w(TAG, "sendRawInput not implemented - use specific HID methods")
        return false
    }
}
