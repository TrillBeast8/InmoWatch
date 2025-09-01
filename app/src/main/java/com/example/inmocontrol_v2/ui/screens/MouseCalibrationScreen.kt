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
import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun MouseCalibrationScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    var minX by remember { mutableStateOf(0f) }
    var minY by remember { mutableStateOf(0f) }
    var maxX by remember { mutableStateOf(1f) }
    var maxY by remember { mutableStateOf(1f) }
    var centerX by remember { mutableStateOf(0.5f) }
    var centerY by remember { mutableStateOf(0.5f) }
    var sensitivity by remember { mutableStateOf(1.0f) }
    var feedbackMessage by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    minX = 0f // Simulate value
                    minY = 0f // Simulate value
                    step++
                }) { Text("Confirm Top-Left") }
            }
            1 -> {
                Text("Step 2: Move mouse to BOTTOM-RIGHT and press Confirm")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    maxX = 1f // Simulate value
                    maxY = 1f // Simulate value
                    step++
                }) { Text("Confirm Bottom-Right") }
            }
            2 -> {
                Text("Step 3: Move mouse to CENTER and press Confirm")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    centerX = 0.5f // Simulate value
                    centerY = 0.5f // Simulate value
                    step++
                }) { Text("Confirm Center") }
            }
            3 -> {
                Text("Step 4: Adjust mouse sensitivity")
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.5f..2.0f,
                    steps = 3
                )
                Text("Sensitivity: %.2f".format(sensitivity))
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { step++ }) { Text("Next") }
            }
            4 -> {
                Text("Step 5: Save calibration")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        settingsStore.setBaselineGyro(gyroValues.value)
                        settingsStore.setBaselineAccel(accelValues.value)
                        settingsStore.setSensitivity(sensitivity)
                        feedbackMessage = "Calibration saved!"
                    }
                }) { Text("Save Calibration") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onNavigate("settings") }) { Text("Back to Settings") }
            }
        }
        if (feedbackMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(feedbackMessage, color = MaterialTheme.colorScheme.primary)
        }
    }
}
