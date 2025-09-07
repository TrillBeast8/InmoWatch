package com.example.inmocontrol_v2.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.data.SettingsStore
import com.example.inmocontrol_v2.sensors.WearMouseSensorFusion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

data class CalibrationState(
    var isCalibrating: Boolean = false,
    var calibrationProgress: Int = 0,
    var maxProgress: Int = 100,
    var message: String = "Hold watch in neutral position",
    var isComplete: Boolean = false
)

@Composable
fun MouseCalibrationScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()
    val calibrationState = remember { mutableStateOf(CalibrationState()) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val minDimension = min(screenWidthDp.value, screenHeightDp.value)
    val calibrationAreaSize = (minDimension * 0.7f).dp

    // WearMouse sensor fusion for calibration
    val sensorFusion = remember { WearMouseSensorFusion(context) }

    // Auto-return to main screen after completion
    LaunchedEffect(calibrationState.value.isComplete) {
        if (calibrationState.value.isComplete) {
            delay(2000)
            onBack()
        }
    }

    // Calibration process
    fun startCalibration() {
        scope.launch {
            calibrationState.value = calibrationState.value.copy(
                isCalibrating = true,
                calibrationProgress = 0,
                message = "Hold watch steady in neutral position"
            )

            try {
                // Configure sensor fusion for calibration
                sensorFusion.setHandMode(WearMouseSensorFusion.HandMode.CENTER)
                sensorFusion.setStabilize(true)
                sensorFusion.setLefty(false)

                // Collect baseline data for 3 seconds
                var sampleCount = 0
                val maxSamples = 100 // About 3 seconds at normal rate

                // Start sensor fusion with callback
                sensorFusion.start { movement ->
                    if (calibrationState.value.isCalibrating) {
                        sampleCount++
                        val progress = (sampleCount * 100) / maxSamples

                        calibrationState.value = calibrationState.value.copy(
                            calibrationProgress = progress,
                            message = when {
                                progress < 30 -> "Collecting baseline data..."
                                progress < 60 -> "Keep holding steady..."
                                progress < 90 -> "Almost done..."
                                else -> "Finalizing calibration..."
                            }
                        )

                        if (sampleCount >= maxSamples) {
                            // Save calibration baseline
                            scope.launch {
                                settingsStore.saveMouseCalibrationComplete(true)
                            }

                            calibrationState.value = calibrationState.value.copy(
                                isCalibrating = false,
                                calibrationProgress = 100,
                                message = "Calibration Complete!",
                                isComplete = true
                            )

                            sensorFusion.stop()
                        }
                    }
                }

                // Wait for calibration to complete or timeout after 10 seconds
                var timeoutCount = 0
                while (calibrationState.value.isCalibrating && timeoutCount < 100) {
                    delay(100)
                    timeoutCount++
                }

                if (timeoutCount >= 100) {
                    // Timeout
                    calibrationState.value = calibrationState.value.copy(
                        isCalibrating = false,
                        message = "Calibration timeout. Try again.",
                        isComplete = false
                    )
                    sensorFusion.stop()
                }

            } catch (e: Exception) {
                Log.e("MouseCalibration", "Calibration failed: ${e.message}")
                calibrationState.value = calibrationState.value.copy(
                    isCalibrating = false,
                    message = "Calibration failed. Try again.",
                    isComplete = false
                )
                sensorFusion.stop()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sensorFusion.stop()
        }
    }

    Scaffold {
        TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                WearText(
                    text = "WearMouse Calibration",
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontSize = 14.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center
                )

                // Calibration area with progress indicator
                Box(
                    modifier = Modifier
                        .size(calibrationAreaSize)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f

                        // Background circle
                        drawCircle(
                            color = Color(0xFF404040),
                            radius = radius - 4.dp.toPx(),
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Progress arc
                        if (calibrationState.value.calibrationProgress > 0) {
                            val sweepAngle = (calibrationState.value.calibrationProgress / 100f) * 360f
                            drawArc(
                                color = if (calibrationState.value.isComplete) Color(0xFF4CAF50) else Color(0xFF2196F3),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - 8.dp.toPx(),
                                    size.height - 8.dp.toPx()
                                ),
                                style = Stroke(width = 6.dp.toPx())
                            )
                        }

                        // Center indicator
                        drawCircle(
                            color = if (calibrationState.value.isCalibrating) Color(0xFF2196F3) else Color(0xFF666666),
                            radius = 8.dp.toPx(),
                            center = center
                        )
                    }

                    // Progress percentage
                    if (calibrationState.value.calibrationProgress > 0) {
                        WearText(
                            text = "${calibrationState.value.calibrationProgress}%",
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status message
                WearText(
                    text = calibrationState.value.message,
                    fontSize = 12.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Control buttons
                if (!calibrationState.value.isCalibrating && !calibrationState.value.isComplete) {
                    Button(
                        onClick = { startCalibration() },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        WearText("Start Calibration", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        WearText("Back", fontSize = 12.sp)
                    }
                }

                if (calibrationState.value.isComplete) {
                    WearText(
                        text = "Returning to main screen...",
                        fontSize = 10.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun MouseCalibrationScreenPreview() {
    MouseCalibrationScreen()
}
