package com.example.inmocontrol_v2.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.data.SettingsStore
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

@Composable
fun MouseScreen() {
    val coroutineScope = rememberCoroutineScope()
    val lastAction = remember { mutableStateOf("Tilt to move") }
    val isMoving = remember { mutableStateOf(false) }
    val actionFeedbackVisible = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val configuration = LocalConfiguration.current

    // Calculate sizes based on screen dimensions
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val minDimension = min(screenWidthDp.value, screenHeightDp.value)

    // Mouse area takes most of the screen
    val mouseAreaSize = (minDimension * 0.9f).dp
    // Center indicator is smaller
    val indicatorSize = (minDimension * 0.3f).dp

    // Sensor and settings state
    val settingsStore = remember { SettingsStore.get(context) }
    val baselineGyro = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val baselineAccel = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val sensitivity = settingsStore.sensitivity.collectAsState(initial = 10f).value

    // Sensor values
    val accelValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val gyroValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }

    // Sensor event processing
    val sensorEventChannel = remember { Channel<Pair<Float, Float>>(capacity = Channel.UNLIMITED) }
    val job = remember { Job() }

    // Load calibration baselines
    LaunchedEffect(Unit) {
        settingsStore.baselineGyro.collectLatest { baselineGyro.value = it }
    }
    LaunchedEffect(Unit) {
        settingsStore.baselineAccel.collectLatest { baselineAccel.value = it }
    }

    // Auto-hide action feedback after 2 seconds
    LaunchedEffect(actionFeedbackVisible.value) {
        if (actionFeedbackVisible.value) {
            delay(2000)
            actionFeedbackVisible.value = false
        }
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
                        // Sensor fusion: 98% gyro, 2% accel for stability
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

    // Batch and process mouse movement
    LaunchedEffect(Unit) {
        val movementThreshold = 0.05f
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
            if (abs(deltaX) > movementThreshold || abs(deltaY) > movementThreshold) {
                isMoving.value = true
                lastAction.value = "Moving..."
                actionFeedbackVisible.value = true
                coroutineScope.launch {
                    try {
                        HidClient.instance()?.moveMouse((deltaX * sensitivity).toInt(), (deltaY * sensitivity).toInt())
                    } catch (e: Exception) {}
                }

                // Stop showing movement after a brief delay
                coroutineScope.launch {
                    delay(200)
                    isMoving.value = false
                }
            }
        }
    }

    Scaffold {
        TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main mouse area - circular region for interactions
            Box(
                modifier = Modifier
                    .size(mouseAreaSize)
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                coroutineScope.launch {
                                    lastAction.value = "Left Click"
                                    actionFeedbackVisible.value = true
                                    try {
                                        HidClient.instance()?.mouseLeftClick()
                                    } catch (e: Exception) {}
                                }
                            },
                            onLongPress = {
                                coroutineScope.launch {
                                    lastAction.value = "Right Click"
                                    actionFeedbackVisible.value = true
                                    try {
                                        HidClient.instance()?.mouseRightClick()
                                    } catch (e: Exception) {}
                                }
                            },
                            onDoubleTap = {
                                coroutineScope.launch {
                                    lastAction.value = "Double Click"
                                    actionFeedbackVisible.value = true
                                    try {
                                        HidClient.instance()?.mouseDoubleClick()
                                    } catch (e: Exception) {}
                                }
                            }
                        )
                    }
            ) {
                // Visual feedback ring around the mouse area
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Outer ring - mouse area boundary
                    drawCircle(
                        color = if (isMoving.value) Color.Cyan.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.4f),
                        radius = radius - 4.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Inner crosshairs for reference
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(center.x - 20.dp.toPx(), center.y),
                        end = Offset(center.x + 20.dp.toPx(), center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(center.x, center.y - 20.dp.toPx()),
                        end = Offset(center.x, center.y + 20.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Center indicator circle showing sensor status and actions
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .clip(CircleShape)
                    .background(
                        when {
                            isMoving.value -> Color.Cyan.copy(alpha = 0.8f)
                            actionFeedbackVisible.value -> Color.Green.copy(alpha = 0.8f)
                            else -> Color.Blue.copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                WearText(
                    text = lastAction.value,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.padding(6.dp),
                    maxLines = 2
                )
            }

            // Instruction text at the top
            WearText(
                text = "Motion Mouse",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            // Sensitivity indicator at bottom
            WearText(
                text = "Sensitivity: ${sensitivity.toInt()}",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }

    // Set input mode to MOUSE when screen loads
    LaunchedEffect(Unit) {
        HidClient.instance()?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.MOUSE)
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun MouseScreenPreview() {
    MouseScreen()
}
