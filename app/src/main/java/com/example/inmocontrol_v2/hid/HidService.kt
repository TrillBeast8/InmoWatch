package com.example.inmocontrol_v2.hid

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.MainThread
import com.example.inmocontrol_v2.bluetooth.HidDeviceApp
import com.example.inmocontrol_v2.bluetooth.HidDeviceProfile
import com.example.inmocontrol_v2.bluetooth.HidInputManager
import com.example.inmocontrol_v2.bluetooth.InputProfile
import com.example.inmocontrol_v2.bluetooth.GenericInputProfile
import com.example.inmocontrol_v2.bluetooth.InmoAir2InputProfile
import com.example.inmocontrol_v2.data.DeviceProfile

/**
 * HID Service implementation - optimized for fast initialization and seamless connections
 * Based on proven WearMouse pattern to eliminate delays and connectivity issues
 */
class HidService : Service(), HidServiceApi {

    companion object {
        private const val TAG = "HidService"
    }

    // Service binder
    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    // Core HID components - fast initialization
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var hidDeviceProfile: HidDeviceProfile
    private lateinit var hidDeviceApp: HidDeviceApp
    private lateinit var hidInputManager: HidInputManager
    private lateinit var inputProfile: InputProfile

    // Service state
    override var currentDeviceProfile: DeviceProfile? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isServiceReady = false
    private var isAppRegistered = false
    private var isProfileConnected = false

    // Service lifecycle
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HidService onCreate - fast initialization")
        initializeBluetoothComponents()
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HidService onDestroy")
        cleanup()
    }

    /**
     * Fast Bluetooth initialization - no delays or retries
     */
    private fun initializeBluetoothComponents() {
        try {
            // Get Bluetooth adapter immediately
            val bluetoothManager = getSystemService(BluetoothManager::class.java)
            bluetoothAdapter = bluetoothManager?.adapter
                ?: throw IllegalStateException("Bluetooth not available")

            if (!bluetoothAdapter.isEnabled) {
                Log.w(TAG, "Bluetooth is not enabled")
                return
            }

            // Initialize HID components
            hidDeviceProfile = HidDeviceProfile()
            hidDeviceApp = HidDeviceApp()
            hidInputManager = HidInputManager(hidDeviceProfile)
            inputProfile = GenericInputProfile(hidInputManager)

            // Set up listeners
            setupListeners()

            // Register HID profile immediately - no delays
            hidDeviceProfile.registerServiceListener(this, serviceStateListener)

            Log.d(TAG, "Bluetooth components initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Bluetooth components", e)
            // Notify HidClient of the error
            HidClient.setConnectionError("Bluetooth initialization failed: ${e.message}")
        }
    }

    /**
     * Set up event listeners for seamless operation
     */
    private fun setupListeners() {
        hidDeviceApp.setDeviceStateListener(deviceStateListener)
        hidDeviceProfile.registerDeviceListener(profileDeviceStateListener)
    }

    /**
     * Service state listener for HID profile connection - fast response
     */
    private val serviceStateListener = object : HidDeviceProfile.ServiceStateListener {
        @MainThread
        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            Log.d(TAG, "HID profile service state changed: ${proxy != null}")

            isProfileConnected = proxy != null

            if (proxy != null) {
                // Profile connected - register app immediately
                isServiceReady = true
                registerHidApp(proxy)
            } else {
                // Profile disconnected
                isServiceReady = false
                isAppRegistered = false
            }

            // Notify HidClient of profile availability immediately
            HidClient.setHidProfileAvailable(proxy != null)
            updateServiceReadyState()
        }
    }

    /**
     * Device state listener for connection events - seamless handling
     */
    private val deviceStateListener = object : HidDeviceApp.DeviceStateListener {
        @MainThread
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "Device connection state changed: ${device.address} -> $state")

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    HidClient.lastConnectedDevice = device
                    HidClient.clearError()
                    Log.d(TAG, "Device connected successfully: ${device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device.address) {
                        connectedDevice = null
                    }
                    Log.d(TAG, "Device disconnected: ${device.address}")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d(TAG, "Device connecting: ${device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    Log.d(TAG, "Device disconnecting: ${device.address}")
                }
            }

            // Update connection state immediately
            updateConnectionState()
        }

        @MainThread
        override fun onAppStatusChanged(registered: Boolean) {
            Log.d(TAG, "HID app status changed: $registered")
            isAppRegistered = registered
            updateServiceReadyState()
        }
    }

    /**
     * Profile device state listener for additional connection events
     */
    private val profileDeviceStateListener = object : HidDeviceProfile.DeviceStateListener {
        @MainThread
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "Profile device state changed: ${device.address} -> $state")
            // This provides additional connection state updates from the profile level
        }

        @MainThread
        override fun onAppStatusChanged(device: BluetoothDevice, registered: Boolean) {
            Log.d(TAG, "Profile app status changed: ${device.address} -> $registered")
            // Additional app status updates from profile level
        }
    }

    /**
     * Register HID app immediately when profile is ready
     */
    private fun registerHidApp(proxy: BluetoothProfile) {
        try {
            val success = hidDeviceApp.registerApp(proxy)
            Log.d(TAG, "HID app registration result: $success")

            if (success) {
                isAppRegistered = true
                updateServiceReadyState()
            } else {
                HidClient.setConnectionError("Failed to register HID app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register HID app", e)
            HidClient.setConnectionError("HID app registration error: ${e.message}")
        }
    }

    /**
     * Update service ready state and notify clients
     */
    private fun updateServiceReadyState() {
        val wasReady = isServiceReady
        isServiceReady = isProfileConnected && isAppRegistered

        if (isServiceReady != wasReady) {
            Log.d(TAG, "Service ready state changed: $isServiceReady")
            // The HidClient will pick up this change through its StateFlow monitoring
        }
    }

    /**
     * Update connection state and notify clients
     */
    private fun updateConnectionState() {
        // HidClient will automatically pick up connection state changes
        // through its StateFlow monitoring and the service ready state
    }

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        try {
            hidDeviceApp.unregisterApp()
            hidDeviceProfile.unregisterServiceListener()
            isServiceReady = false
            isAppRegistered = false
            isProfileConnected = false
            connectedDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    // HidServiceApi implementation
    override fun isReady(): Boolean = isServiceReady && isAppRegistered

    override fun isDeviceConnected(): Boolean = connectedDevice != null

    override fun connect(device: BluetoothDevice): Boolean {
        return if (isReady()) {
            Log.d(TAG, "Connecting to device: ${device.address}")
            // Update input profile based on device profile
            updateInputProfile()
            val success = hidDeviceApp.requestConnect(device)
            if (!success) {
                HidClient.setConnectionError("Failed to initiate connection to ${device.address}")
            }
            success
        } else {
            Log.w(TAG, "Service not ready for connection. Ready: $isServiceReady, Registered: $isAppRegistered")
            HidClient.setConnectionError("Service not ready for connection")
            false
        }
    }

    override fun disconnect(): Boolean {
        return connectedDevice?.let { device ->
            Log.d(TAG, "Disconnecting from device: ${device.address}")
            hidDeviceApp.requestDisconnect(device)
        } ?: false
    }

    override fun getConnectedDevice(): BluetoothDevice? = connectedDevice

    override fun getConnectedDevices(): List<BluetoothDevice> =
        hidDeviceApp.getConnectedDevices()

    /**
     * Update input profile based on current device profile
     */
    private fun updateInputProfile() {
        inputProfile = when (currentDeviceProfile) {
            DeviceProfile.InmoAir2 -> InmoAir2InputProfile(hidInputManager)
            else -> GenericInputProfile(hidInputManager)
        }
        Log.d(TAG, "Updated input profile for device type: $currentDeviceProfile")
    }

    // Input methods - delegate to current input profile with error handling
    override fun sendKey(keyCode: Int, modifiers: Int): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.sendKey(device, keyCode, modifiers)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending key", e)
                false
            }
        } ?: false
    }

    override fun moveMouse(dx: Int, dy: Int): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.moveMouse(device, dx, dy)
            } catch (e: Exception) {
                Log.e(TAG, "Error moving mouse", e)
                false
            }
        } ?: false
    }

    override fun mouseScroll(x: Int, y: Int): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.mouseScroll(device, x, y)
            } catch (e: Exception) {
                Log.e(TAG, "Error scrolling mouse", e)
                false
            }
        } ?: false
    }

    override fun mouseLeftClick(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.mouseLeftClick(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error left clicking", e)
                false
            }
        } ?: false
    }

    override fun mouseRightClick(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.mouseRightClick(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error right clicking", e)
                false
            }
        } ?: false
    }

    override fun mouseDoubleClick(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.mouseDoubleClick(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error double clicking", e)
                false
            }
        } ?: false
    }

    override fun mouseDragMove(dx: Int, dy: Int): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.mouseDragMove(device, dx, dy)
            } catch (e: Exception) {
                Log.e(TAG, "Error drag moving", e)
                false
            }
        } ?: false
    }

    override fun mouseDragEnd(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.mouseDragEnd(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error ending drag", e)
                false
            }
        } ?: false
    }

    override fun dpad(direction: Int): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.dpad(device, direction)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending dpad", e)
                false
            }
        } ?: false
    }

    override fun playPause(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.playPause(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error play/pause", e)
                false
            }
        } ?: false
    }

    override fun nextTrack(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.nextTrack(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error next track", e)
                false
            }
        } ?: false
    }

    override fun previousTrack(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.previousTrack(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error previous track", e)
                false
            }
        } ?: false
    }

    override fun setVolume(volume: Int): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.setVolume(device, volume)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting volume", e)
                false
            }
        } ?: false
    }

    override fun volumeUp(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.volumeUp(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error volume up", e)
                false
            }
        } ?: false
    }

    override fun volumeDown(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.volumeDown(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error volume down", e)
                false
            }
        } ?: false
    }

    override fun switchOutput(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.switchOutput(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error switching output", e)
                false
            }
        } ?: false
    }

    override fun resetInputStates(): Boolean {
        return connectedDevice?.let { device ->
            try {
                inputProfile.resetInputs(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting inputs", e)
                false
            }
        } ?: false
    }
}
