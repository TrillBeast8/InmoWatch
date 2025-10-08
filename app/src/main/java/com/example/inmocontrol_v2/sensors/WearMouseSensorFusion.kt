package com.example.inmocontrol_v2.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.*

/**
 * Highly optimized WearMouse sensor fusion with improved performance
 * Reduced memory allocations and faster mathematical operations
 */
class WearMouseSensorFusion(context: Context) {

    companion object {
        private const val CURSOR_SPEED = 1024.0 / (PI / 4)
        private const val STABILIZE_BIAS = 16.0
        private const val DATA_RATE_US = 11250

        // Pre-computed constants for performance
        private const val CURSOR_SPEED_FLOAT = CURSOR_SPEED.toFloat()
        private const val STABILIZE_BIAS_FLOAT = STABILIZE_BIAS.toFloat()
        private const val PI_HALF = (PI / 2).toFloat()
        private const val TWO_PI = (2 * PI).toFloat()
    }

    enum class HandMode { LEFT, CENTER, RIGHT }

    // Inline class for better performance
    @JvmInline
    value class MouseMovement(private val packed: Long) {
        constructor(deltaX: Float, deltaY: Float, deltaWheel: Float = 0f) : this(
            (deltaX.toBits().toLong() shl 32) or
            (deltaY.toBits().toLong() and 0xFFFFFFFFL)
        )

        val deltaX: Float get() = Float.fromBits((packed shr 32).toInt())
        val deltaY: Float get() = Float.fromBits((packed and 0xFFFFFFFFL).toInt())
    }

    // Cached sensor manager and sensors
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val preferredSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // State variables with better precision
    @Volatile private var yaw = 0f
    @Volatile private var pitch = 0f
    @Volatile private var dYaw = 0f
    @Volatile private var dPitch = 0f
    @Volatile private var dWheel = 0f
    @Volatile private var firstRead = true
    @Volatile private var isActive = false

    // Optimized settings
    private var handMode = HandMode.CENTER
    private var stabilize = true
    private var lefty = false

    // Callback with better type safety
    private var onMovement: ((MouseMovement) -> Unit)? = null

    // Pre-allocated arrays to avoid garbage collection
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Optimized sensor listener with minimal allocations
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!isActive) return
            processOrientation(event.values)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start(onMovement: (MouseMovement) -> Unit) {
        this.onMovement = onMovement
        resetState()

        preferredSensor?.let { sensor ->
            isActive = true
            sensorManager.registerListener(sensorListener, sensor, DATA_RATE_US)
        }
    }

    fun stop() {
        isActive = false
        onMovement = null
        sensorManager.unregisterListener(sensorListener)
    }

    private fun resetState() {
        yaw = 0f
        pitch = 0f
        dYaw = 0f
        dPitch = 0f
        dWheel = 0f
        firstRead = true
    }

    // Optimized orientation processing with fewer allocations
    private fun processOrientation(rotationVector: FloatArray) {
        // Convert rotation vector to rotation matrix (optimized)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        // Get orientation from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientation)

        val newYaw = orientation[0]
        val newPitch = orientation[1]

        if (firstRead) {
            yaw = newYaw
            pitch = newPitch
            firstRead = false
            return
        }

        // Calculate deltas with wraparound handling (optimized)
        var deltaYaw = newYaw - yaw
        var deltaPitch = newPitch - pitch

        // Handle wraparound efficiently
        if (deltaYaw > PI) deltaYaw -= TWO_PI
        else if (deltaYaw < -PI) deltaYaw += TWO_PI

        if (deltaPitch > PI_HALF) deltaPitch -= PI.toFloat()
        else if (deltaPitch < -PI_HALF) deltaPitch += PI.toFloat()

        // Apply stabilization if enabled
        if (stabilize) {
            deltaYaw = stabilizeValue(deltaYaw)
            deltaPitch = stabilizeValue(deltaPitch)
        }

        // Update state
        yaw = newYaw
        pitch = newPitch

        // Calculate cursor movement (optimized math)
        val cursorX = deltaYaw * CURSOR_SPEED_FLOAT
        val cursorY = deltaPitch * CURSOR_SPEED_FLOAT

        // Apply hand mode corrections with optimized calculations
        val (finalX, finalY) = when (handMode) {
            HandMode.LEFT -> Pair(-cursorY, cursorX)
            HandMode.RIGHT -> Pair(cursorY, -cursorX)
            HandMode.CENTER -> Pair(cursorX, cursorY)
        }

        // Apply lefty correction if needed
        val adjustedX = if (lefty) -finalX else finalX

        // Send movement only if significant enough to reduce unnecessary calls
        if (abs(adjustedX) > 0.5f || abs(finalY) > 0.5f) {
            onMovement?.invoke(MouseMovement(adjustedX, finalY))
        }
    }

    // Optimized stabilization function
    private fun stabilizeValue(value: Float): Float {
        return if (abs(value) < STABILIZE_BIAS_FLOAT) {
            value * (abs(value) / STABILIZE_BIAS_FLOAT)
        } else {
            value
        }
    }

    // Configuration methods
    fun setHandMode(mode: HandMode) {
        handMode = mode
    }

    fun setStabilize(enabled: Boolean) {
        stabilize = enabled
    }

    fun setLefty(enabled: Boolean) {
        lefty = enabled
    }
}
