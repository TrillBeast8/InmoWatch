package com.example.inmocontrol_v2.ui.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.launch

data class CalibrationState(
    var minX: Float = 0f,
    var minY: Float = 0f,
    var maxX: Float = 1f,
    var maxY: Float = 1f,
    var centerX: Float = 0.5f,
    var centerY: Float = 0.5f,
    var sensitivity: Float = 1.0f
)

@Composable
fun MouseCalibrationScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    var feedbackMessage by remember { mutableStateOf("") }
    val calibrationState = remember { mutableStateOf(CalibrationState()) }

    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager }
    val accelValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val gyroValues = remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    DisposableEffect(Unit) {
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> accelValues.value = event.values.clone()
                    Sensor.TYPE_GYROSCOPE -> gyroValues.value = event.values.clone()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mouse Calibration", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            when (step) {
                0 -> {
                    Text("Step 1: Move mouse to TOP-LEFT and press Confirm")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        calibrationState.value.minX = 0f
                        calibrationState.value.minY = 0f
                        step++
                    }) { Text("Confirm Top-Left") }
                }
                1 -> {
                    Text("Step 2: Move mouse to BOTTOM-RIGHT and press Confirm")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        calibrationState.value.maxX = 1f
                        calibrationState.value.maxY = 1f
                        step++
                    }) { Text("Confirm Bottom-Right") }
                }
                2 -> {
                    Text("Step 3: Move mouse to CENTER and press Confirm")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        calibrationState.value.centerX = 0.5f
                        calibrationState.value.centerY = 0.5f
                        step++
                    }) { Text("Confirm Center") }
                }
                3 -> {
                    Text("Step 4: Adjust mouse sensitivity")
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = calibrationState.value.sensitivity,
                        onValueChange = { calibrationState.value.sensitivity = it },
                        valueRange = 0.1f..3.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Sensitivity: ${String.format("%.2f", calibrationState.value.sensitivity)}")
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                // Save calibration and baselines
                                settingsStore.saveCalibration(
                                    calibrationState.value.minX,
                                    calibrationState.value.minY,
                                    calibrationState.value.maxX,
                                    calibrationState.value.maxY,
                                    calibrationState.value.centerX,
                                    calibrationState.value.centerY,
                                    calibrationState.value.sensitivity
                                )
                                // Save current sensor readings as baselines
                                settingsStore.setBaselineGyro(gyroValues.value)
                                settingsStore.setBaselineAccel(accelValues.value)
                                feedbackMessage = "Calibration saved!"
                            }
                        }) {
                            Text("Save")
                        }

                        Button(onClick = {
                            onNavigate("settings")
                        }) {
                            Text("Back")
                        }
                    }
                }
                else -> {
                    Text("Calibration Complete!")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onNavigate("settings") }) {
                        Text("Back to Settings")
                    }
                }
            }

            if (feedbackMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(feedbackMessage, color = MaterialTheme.colorScheme.primary)
                LaunchedEffect(feedbackMessage) {
                    kotlinx.coroutines.delay(2000)
                    feedbackMessage = ""
                }
            }

            // Show current sensor values for debugging
            if (step < 4) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Gyro: ${String.format("%.2f, %.2f, %.2f",
                        gyroValues.value[0], gyroValues.value[1], gyroValues.value[2])}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Accel: ${String.format("%.2f, %.2f, %.2f",
                        accelValues.value[0], accelValues.value[1], accelValues.value[2])}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(
    name = "Wear OS Round Preview",
    device = "id:wearos_small_round",
    showBackground = true
)
@Composable
fun MouseCalibrationScreenPreview() {
    MouseCalibrationScreen()
}
