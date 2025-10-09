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
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        // Retry registerApp() with exponential backoff
                        serviceScope.launch {
                            retryRegisterApp()
                        }
                    }
                }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            isAppRegistered = registered
            HidClient.setHidProfileAvailable(registered)
            Log.d(TAG, "App registration status: $registered")
        }
    }

    // Service state listener
    private val serviceStateListener = object : HidDeviceProfile.ServiceStateListener {
        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            isServiceReady = proxy != null
            HidClient.setServiceReady(isServiceReady)
            HidClient.setHidProfileAvailable(isServiceReady)

            if (isServiceReady) {
                // Register HID app when profile is ready
                serviceScope.launch {
                    delay(100) // Small delay for stability
                    hidDeviceProfile.registerApp(hidDeviceApp)
                }
            }

            Log.d(TAG, "HID service state changed: $isServiceReady")
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
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Bluetooth HID connection active"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InmoWatch HID")
            .setContentText("HID controller active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
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
        if (!isServiceReady || !isAppRegistered) {
            Log.w(TAG, "Service not ready or app not registered; cannot connect")
            return false
        }
        return hidDeviceProfile.connect(device)
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

    // Raw HID
    override fun sendRawInput(data: ByteArray): Boolean {
        // Not implemented - use specific methods instead
        Log.w(TAG, "sendRawInput not implemented - use specific HID methods")
        return false
    }
}
