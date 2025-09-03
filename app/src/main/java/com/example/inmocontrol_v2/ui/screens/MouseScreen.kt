package com.example.inmocontrol_v2.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.example.inmocontrol_v2.data.SettingsStore
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun MouseScreen() {
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    val lastAction = remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    // Click feedback state
    val clickFeedback = remember { mutableStateOf<String?>(null) }
    val feedbackTimestamp = remember { mutableStateOf(0L) }

    // Coroutine scope for sensor event batching
    val sensorEventChannel = remember { Channel<Pair<Float, Float>>(capacity = Channel.UNLIMITED) }
    val job = remember { Job() }

    // State for accelerometer and gyroscope
    val accelValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val gyroValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }

    // Persistent calibration baselines
    val settingsStore = remember { SettingsStore.get(context) }
    val baselineGyro = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val baselineAccel = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val sensitivity = remember { mutableStateOf(10f) }

    LaunchedEffect(Unit) {
        settingsStore.baselineGyro.collectLatest { baselineGyro.value = it }
        settingsStore.baselineAccel.collectLatest { baselineAccel.value = it }
        settingsStore.sensitivity.collectLatest { sensitivity.value = it }
    }

    // Sensor-based mouse movement (fusion)
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        accelValues.value = event.values.clone()
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        gyroValues.value = event.values.clone()
                        // Use persistent calibration baselines
                        val adjGyro = floatArrayOf(
                            event.values[0] - baselineGyro.value[0],
                            event.values[1] - baselineGyro.value[1],
                            event.values[2] - baselineGyro.value[2]
                        )
                        val adjAccel = floatArrayOf(
                            accelValues.value[0] - baselineAccel.value[0],
                            accelValues.value[1] - baselineAccel.value[1],
                            accelValues.value[2] - baselineAccel.value[2]
                        )
                        val fusedX = 0.98f * adjGyro[0] + 0.02f * adjAccel[0]
                        val fusedY = 0.98f * adjGyro[1] + 0.02f * adjAccel[1]
                        sensorEventChannel.trySend(Pair(fusedX, fusedY))
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
            job.cancel()
        }
    }

    // Batch and debounce mouse movement
    LaunchedEffect(Unit) {
        val movementThreshold = 0.05f // Only update if movement is significant
        while (true) {
            var deltaX = 0f
            var deltaY = 0f
            // Collect events for 50ms
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 50) {
                val event = sensorEventChannel.tryReceive().getOrNull()
                if (event != null) {
                    deltaX += event.first
                    deltaY += event.second
                } else {
                    delay(5)
                }
            }
            if (kotlin.math.abs(deltaX) > movementThreshold || kotlin.math.abs(deltaY) > movementThreshold) {
                offsetX.value += deltaX * sensitivity.value
                offsetY.value += deltaY * sensitivity.value
                HidClient.instance()?.moveMouse((deltaX * sensitivity.value).toInt(), (deltaY * sensitivity.value).toInt())
                lastAction.value = "Move"
            }
        }
    }

    Scaffold {
        TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            val boxSize = 200.dp
            val boxPx = with(androidx.compose.ui.platform.LocalDensity.current) { boxSize.toPx() }
            // Clamp offset to stay within box
            val clampedX = offsetX.value.coerceIn(-boxPx / 2 + 12, boxPx / 2 - 12)
            val clampedY = offsetY.value.coerceIn(-boxPx / 2 + 12, boxPx / 2 - 12)
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                HidClient.instance()?.mouseLeftClick()
                                clickFeedback.value = "Left Click"
                                feedbackTimestamp.value = System.currentTimeMillis()
                                lastAction.value = "Left Click"
                            },
                            onDoubleTap = {
                                HidClient.instance()?.mouseDoubleClick()
                                clickFeedback.value = "Double Click"
                                feedbackTimestamp.value = System.currentTimeMillis()
                                lastAction.value = "Double Click"
                            },
                            onLongPress = {
                                HidClient.instance()?.mouseRightClick()
                                clickFeedback.value = "Right Click"
                                feedbackTimestamp.value = System.currentTimeMillis()
                                lastAction.value = "Right Click"
                            }
                        )
                    }
            ) {
                // Moving circle indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset {
                            IntOffset(
                                clampedX.roundToInt(),
                                clampedY.roundToInt()
                            )
                        }
                        .clip(CircleShape)
                        .background(Color.Blue.copy(alpha = 0.7f))
                )
                // Static origin marker (dot)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(androidx.compose.ui.Alignment.Center)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                // Mouse indicator (movable)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                // Click feedback indicator
                if (clickFeedback.value != null && System.currentTimeMillis() - feedbackTimestamp.value < 500) {
                    val color = when (clickFeedback.value) {
                        "Left Click" -> Color.Green
                        "Right Click" -> Color.Red
                        "Double Click" -> Color.Blue
                        else -> Color.Yellow
                    }
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.Center)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.4f))
                    )
                }
            }
            // Show last action at the bottom
            androidx.wear.compose.material.Text(
                text = "Last Action: ${lastAction.value}",
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, device = "id:wearos_small_round")
@Composable
fun MouseScreenPreview() {
    MouseScreen()
}
