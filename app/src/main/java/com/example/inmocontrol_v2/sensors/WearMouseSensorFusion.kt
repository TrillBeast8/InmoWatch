package com.example.inmocontrol_v2.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.*

/**
 * Kotlin adaptation of original WearMouse sensor fusion
 * Based on the exact algorithm from WearMouse MouseSensorListener
 */
class WearMouseSensorFusion(private val context: Context) {

    companion object {
        private const val TAG = "WearMouseSensorFusion"
        private const val CURSOR_SPEED = 1024.0 / (PI / 4)  // Exact value from original
        private const val STABILIZE_BIAS = 16.0              // Exact value from original
        private const val DATA_RATE_US = 11250               // Exact value from original
    }

    enum class HandMode { LEFT, CENTER, RIGHT }

    data class MouseMovement(
        val deltaX: Float,
        val deltaY: Float,
        val deltaWheel: Float = 0f
    )

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gameRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // State variables - exactly matching original WearMouse
    private var yaw = 0.0
    private var pitch = 0.0
    private var dYaw = 0.0
    private var dPitch = 0.0
    private var dWheel = 0.0
    private var firstRead = true
    private var isActive = false

    // Settings - exactly matching original
    private var handMode = HandMode.CENTER
    private var stabilize = true
    private var lefty = false

    // Movement callback
    private var onMovement: ((MouseMovement) -> Unit)? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!isActive || onMovement == null) return

            when (event.sensor.type) {
                Sensor.TYPE_GAME_ROTATION_VECTOR,
                Sensor.TYPE_ROTATION_VECTOR -> {
                    onOrientation(event.values)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start(onMovement: (MouseMovement) -> Unit) {
        this.onMovement = onMovement
        isActive = true
        firstRead = true

        // Reset state like original WearMouse onCreate()
        yaw = 0.0
        pitch = 0.0
        dYaw = 0.0
        dPitch = 0.0
        dWheel = 0.0

        val sensor = gameRotationVector ?: rotationVector
        if (sensor != null) {
            sensorManager.registerListener(
                sensorListener,
                sensor,
                DATA_RATE_US
            )
            Log.d(TAG, "Started WearMouse sensor fusion with ${sensor.name}")
        } else {
            Log.e(TAG, "No suitable rotation sensor available")
        }
    }

    fun stop() {
        isActive = false
        onMovement = null
        sensorManager.unregisterListener(sensorListener)
        Log.d(TAG, "Stopped WearMouse sensor fusion")
    }

    /**
     * Exact copy of original WearMouse onOrientation method
     */
    private fun onOrientation(quaternion: FloatArray) {
        var q1 = quaternion[0].toDouble() // X * sin(T/2)
        var q2 = quaternion[1].toDouble() // Y * sin(T/2)
        var q3 = quaternion[2].toDouble() // Z * sin(T/2)
        val q0 = if (quaternion.size > 3) quaternion[3].toDouble()
                 else sqrt(1 - (q1*q1 + q2*q2 + q3*q3)) // cos(T/2)

        if (lefty) {
            // Rotate 180 degrees
            q1 = -q1
            q2 = -q2
        }

        when (handMode) {
            HandMode.LEFT -> {
                // Rotate 90 degrees counter-clockwise
                val x = q1
                val y = q2
                q1 = -y
                q2 = x
            }
            HandMode.RIGHT -> {
                // Rotate 90 degrees clockwise
                val x = q1
                val y = q2
                q1 = y
                q2 = -x
            }
            HandMode.CENTER -> {
                // No rotation needed
            }
        }

        // Convert to Euler angles - EXACT formula from original WearMouse
        val newYaw = atan2(2 * (q0 * q3 - q1 * q2), (1 - 2 * (q1 * q1 + q3 * q3)))
        val newPitch = asin(2 * (q0 * q1 + q2 * q3))

        if (newYaw.isNaN() || newPitch.isNaN()) {
            return // Skip NaN values
        }

        if (firstRead) {
            yaw = newYaw
            pitch = newPitch
            firstRead = false
        } else {
            // Apply high-pass filter exactly like original
            val filteredYaw = highpass(yaw, newYaw)
            val filteredPitch = highpass(pitch, newPitch)

            val deltaYaw = clamp(yaw - filteredYaw)
            val deltaPitch = pitch - filteredPitch

            yaw = filteredYaw
            pitch = filteredPitch

            // Accumulate the error locally - exactly like original
            dYaw += deltaYaw
            dPitch += deltaPitch
        }

        sendCurrentState()
    }

    /**
     * Original WearMouse clamp function
     */
    private fun clamp(value: Double): Double {
        var val1 = value
        while (val1 <= -PI) {
            val1 += 2 * PI
        }
        while (val1 >= PI) {
            val1 -= 2 * PI
        }
        return val1
    }

    /**
     * Original WearMouse highpass filter with adaptive alpha
     */
    private fun highpass(oldVal: Double, newVal: Double): Double {
        if (!stabilize) {
            return newVal
        }
        val delta = clamp(oldVal - newVal)
        val alpha = max(0.0, 1 - (abs(delta) * CURSOR_SPEED / STABILIZE_BIAS).pow(3))
        return newVal + alpha * delta
    }

    /**
     * Original WearMouse sendCurrentState with overflow handling
     */
    private fun sendCurrentState() {
        var dX = dYaw * CURSOR_SPEED
        var dY = dPitch * CURSOR_SPEED
        var dZ = dWheel

        // Scale down to fit protocol - exactly like original
        while (abs(dX) > 127 || abs(dY) > 127) {
            dX /= 2
            dY /= 2
        }

        // Only send if there's actual movement
        if (abs(dX) >= 1.0 || abs(dY) >= 1.0 || abs(dZ) >= 1.0) {
            val movement = MouseMovement(
                deltaX = dX.toFloat(),
                deltaY = dY.toFloat(),
                deltaWheel = dZ.toFloat()
            )

            onMovement?.invoke(movement)

            // Clear accumulated values after sending
            dYaw = 0.0
            dPitch = 0.0
            dWheel = 0.0
        }
    }

    // Setter methods that MouseScreen expects
    fun setHandMode(mode: HandMode) {
        handMode = mode
    }

    fun setStabilize(enable: Boolean) {
        stabilize = enable
    }

    fun setLefty(enable: Boolean) {
        lefty = enable
    }

    fun reset() {
        firstRead = true
        yaw = 0.0
        pitch = 0.0
        dYaw = 0.0
        dPitch = 0.0
        dWheel = 0.0
    }
}
