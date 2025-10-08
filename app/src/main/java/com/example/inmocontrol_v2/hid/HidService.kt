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
import kotlinx.coroutines.*
import com.example.inmocontrol_v2.bluetooth.HidDeviceApp
import com.example.inmocontrol_v2.bluetooth.HidDeviceProfile
import com.example.inmocontrol_v2.bluetooth.HidInputManager
import com.example.inmocontrol_v2.data.DeviceProfile

/**
 * Optimized HID Service with performance improvements and memory management
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

    // Use coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var hidDeviceProfile: HidDeviceProfile
    private lateinit var hidDeviceApp: HidDeviceApp
    private lateinit var hidInputManager: HidInputManager

    private var connectedDevice: BluetoothDevice? = null
    private var isServiceReady = false
    private var isAppRegistered = false

    override var currentDeviceProfile: DeviceProfile? = null
        set(value) {
            field = value
            // No need to create new profile instances - they delegate to hidInputManager
            Log.d(TAG, "Device profile set to: $value")
        }

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

    // Optimized device state listener
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
                }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            isAppRegistered = registered
            HidClient.setHidProfileAvailable(registered)
            Log.d(TAG, "App registration status: $registered")
        }
    }

    // Optimized service state listener
    private val serviceStateListener = object : HidDeviceProfile.ServiceStateListener {
        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            isServiceReady = proxy != null
            HidClient.setHidProfileAvailable(isServiceReady)
            Log.d(TAG, "HID service state changed: $isServiceReady")
        }
    }

    // Profile device state listener
    private val profileDeviceStateListener = object : HidDeviceProfile.DeviceStateListener {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            // Minimal logging for performance
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

    // HidServiceApi implementation - optimized with direct delegation
    override fun isReady(): Boolean = isServiceReady
    override fun isDeviceConnected(): Boolean = connectedDevice != null
    override fun getConnectedDevice(): BluetoothDevice? = connectedDevice

    override fun connectToDevice(device: BluetoothDevice): Boolean {
        return try {
            hidDeviceApp.requestConnect(device)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device", e)
            false
        }
    }

    override fun disconnectFromDevice(): Boolean {
        return try {
            connectedDevice?.let { device ->
                hidDeviceApp.requestDisconnect(device)
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect from device", e)
            false
        }
    }

    override fun startAdvertising(): Boolean {
        return try {
            if (!isAppRegistered) {
                isAppRegistered = true
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start advertising", e)
            false
        }
    }

    override fun stopAdvertising(): Boolean {
        return try {
            if (isAppRegistered) {
                hidDeviceApp.unregisterApp()
                isAppRegistered = false
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop advertising", e)
            false
        }
    }

    // Direct delegation to HidInputManager for better performance
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

    // Missing method implementation
    override fun sendRawInput(data: ByteArray): Boolean =
        hidInputManager.sendHidReport(1, data) // Use keyboard report ID as default

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "HID Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "HID connection service"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InmoControl")
            .setContentText("HID service running")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun cleanup() {
        serviceScope.cancel()
        try {
            connectedDevice?.let { device ->
                hidInputManager.clearAllInputs(device)
            }
            // Clean up HidInputManager coroutines
            hidInputManager.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
