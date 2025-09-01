package com.example.inmocontrol_v2.hid

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

class HidService : Service() {
    private val binder = LocalBinder()
    private var gattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var reportCharacteristic: BluetoothGattCharacteristic

    companion object {
        // HID Service UUID (Bluetooth SIG)
        val HID_SERVICE_UUID: UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        // HID Report Characteristic UUID
        val REPORT_CHAR_UUID: UUID = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb")
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer = manager.openGattServer(this, gattServerCallback)
            setupGattServer()
        } else {
            Log.e("HidService", "BLUETOOTH_CONNECT permission not granted")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer?.close()
        }
    }

    private fun setupGattServer() {
        val hidService = BluetoothGattService(HID_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        reportCharacteristic = BluetoothGattCharacteristic(
            REPORT_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        hidService.addCharacteristic(reportCharacteristic)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer?.addService(hidService)
        } else {
            Log.e("HidService", "BLUETOOTH_CONNECT permission not granted for addService")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                Log.d("HidService", "Device connected: $device")
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevice = null
                Log.d("HidService", "Device disconnected")
            }
        }
    }

    // Mouse report: [buttons, x, y, wheel]
    fun sendMouseReport(buttons: Byte, x: Byte, y: Byte, wheel: Byte = 0) {
        val report = byteArrayOf(buttons, x, y, wheel)
        connectedDevice?.let {
            reportCharacteristic.value = report
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gattServer?.notifyCharacteristicChanged(it, reportCharacteristic, false)
            } else {
                Log.e("HidService", "BLUETOOTH_CONNECT permission not granted for notifyCharacteristicChanged")
            }
        }
    }

    // API for HidClient
    fun moveMouse(dx: Int, dy: Int) {
        sendMouseReport(0, dx.toByte(), dy.toByte())
    }
    fun mouseClick() {
        sendMouseReport(1, 0, 0)
        sendMouseReport(0, 0, 0)
    }
    fun mouseLeftClick() {
        mouseClick()
    }
    fun mouseRightClick() {
        sendMouseReport(2, 0, 0)
        sendMouseReport(0, 0, 0)
    }
    fun mouseDoubleClick() {
        mouseClick(); mouseClick()
    }
    fun mouseDragMove(dx: Int, dy: Int) {
        sendMouseReport(1, dx.toByte(), dy.toByte())
    }
    fun mouseDragEnd() {
        sendMouseReport(0, 0, 0)
    }
    fun mouseScroll(dx: Int, dy: Int) {
        sendMouseReport(0, dx.toByte(), dy.toByte(), dy.toByte())
    }

    fun connect(device: BluetoothDevice) {
        connectedDevice = device
        Log.d("HidService", "Connected to device: $device")
    }

    fun sendReport(report: ByteArray) {
        connectedDevice?.let {
            reportCharacteristic.value = report
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gattServer?.notifyCharacteristicChanged(it, reportCharacteristic, false)
            } else {
                Log.e("HidService", "BLUETOOTH_CONNECT permission not granted for notifyCharacteristicChanged")
            }
        }
    }

    fun sendKeyboardReport(report: ByteArray) {
        sendReport(report)
    }

    // D-pad navigation (arrow keys)
    fun dpad(direction: Int) {
        // direction: 0=up, 1=down, 2=left, 3=right
        val keyCode = when (direction) {
            0 -> 0x52 // Up Arrow
            1 -> 0x51 // Down Arrow
            2 -> 0x50 // Left Arrow
            3 -> 0x4F // Right Arrow
            else -> 0x00
        }
        val report = byteArrayOf(0x00, 0x00, keyCode.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00)
        sendKeyboardReport(report)
        // Release key
        sendKeyboardReport(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
    }

    // Volume control
    fun setVolume(volume: Int) {
        // volume: 1=up, -1=down
        val usage = when (volume) {
            1 -> 0xE9 // Volume Up
            -1 -> 0xEA // Volume Down
            else -> 0x00
        }
        if (usage != 0x00) {
            val report = byteArrayOf(usage.toByte(), 0x00)
            sendReport(report)
            // Release key
            sendReport(byteArrayOf(0x00, 0x00))
        }
    }

    // Media controls
    fun playPause() {
        val report = byteArrayOf(0xCD.toByte(), 0x00) // Play/Pause
        sendReport(report)
        sendReport(byteArrayOf(0x00, 0x00))
    }
    fun previousTrack() {
        val report = byteArrayOf(0xB6.toByte(), 0x00) // Previous Track
        sendReport(report)
        sendReport(byteArrayOf(0x00, 0x00))
    }
    fun nextTrack() {
        val report = byteArrayOf(0xB5.toByte(), 0x00) // Next Track
        sendReport(report)
        sendReport(byteArrayOf(0x00, 0x00))
    }

    // Switch output (Android local + HID custom)
    fun switchOutput(output: Int) {
        // Try Android AudioManager for local output
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        when (output) {
            0 -> { // Headphones
                audioManager?.let {
                    it.isSpeakerphoneOn = false
                    it.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                Log.d("HidService", "Switched to headphones (wired/Bluetooth if available)")
            }
            1 -> { // Speaker
                audioManager?.let {
                    it.isSpeakerphoneOn = true
                    it.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                Log.d("HidService", "Switched to speaker")
            }
            2 -> { // Phone earpiece
                audioManager?.let {
                    it.isSpeakerphoneOn = false
                    it.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                Log.d("HidService", "Switched to phone earpiece")
            }
            else -> {
                Log.d("HidService", "Unknown output device")
            }
        }
        // For remote/Bluetooth HID, send a custom report (if supported)
        val report = byteArrayOf(0x00, output.toByte())
        sendReport(report)
        Log.d("HidService", "Sent HID report for output switch: $output")
    }

    // Send a single key press (keyboard HID report)
    fun sendKey(keyCode: Int) {
        // HID keyboard report: [modifier, reserved, key1, key2, key3, key4, key5, key6]
        // Only key1 is used here
        val report = byteArrayOf(0, 0, keyCode.toByte(), 0, 0, 0, 0, 0)
        sendKeyboardReport(report)
        // Release key
        val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        sendKeyboardReport(releaseReport)
    }
}
