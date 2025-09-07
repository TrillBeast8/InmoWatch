package com.example.inmocontrol_v2.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
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
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.DeviceProfile
import com.example.inmocontrol_v2.sensors.WearMouseSensorFusion
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun MouseScreen(
    onOpenScrollPopup: () -> Unit = {}
) {
    val context = LocalContext.current

    var lastAction by remember { mutableStateOf("Ready") }
    var actionFeedbackVisible by remember { mutableStateOf(false) }
    var showScrollPopup by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current

    // Calculate sizes based on screen dimensions
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val minDimension = min(screenWidthDp.value, screenHeightDp.value)

    // Mouse area takes most of the screen
    val mouseAreaSize = (minDimension * 0.85f).dp

    // WearMouse sensor fusion instance
    val sensorFusion = remember { WearMouseSensorFusion(context) }

    // Use real-time connection state from HidClient
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Auto-hide action feedback after 1.5 seconds
    LaunchedEffect(actionFeedbackVisible) {
        if (actionFeedbackVisible) {
            delay(1500)
            actionFeedbackVisible = false
        }
    }

    // PURE WearMouse sensor fusion - NO touch movement detection
    LaunchedEffect(Unit) {
        // Set device profile to Mouse for proper HID behavior
        HidClient.currentDeviceProfile = DeviceProfile.Mouse

        // Configure sensor fusion exactly like original WearMouse
        sensorFusion.setHandMode(WearMouseSensorFusion.HandMode.CENTER)
        sensorFusion.setStabilize(true)
        sensorFusion.setLefty(false)
        sensorFusion.reset() // Start fresh

        // Start sensor fusion with callback - ONLY source of mouse movement
        try {
            sensorFusion.start { movement ->
                try {
                    // Send pure gyro-based mouse movement - no touch involved
                    if (movement.deltaX != 0f || movement.deltaY != 0f) {
                        HidClient.moveMouse(movement.deltaX.toInt(), movement.deltaY.toInt())
                    }

                    // Handle wheel scrolling if any
                    if (movement.deltaWheel != 0f) {
                        HidClient.mouseScroll(0, movement.deltaWheel.toInt())
                    }
                } catch (e: Exception) {
                    Log.e("MouseScreen", "Error sending gyro movement: ${e.message}")
                }
            }
            Log.d("MouseScreen", "WearMouse sensor fusion started successfully")
        } catch (e: Exception) {
            Log.e("MouseScreen", "Error starting sensor fusion: ${e.message}")
        }
    }

    // Cleanup sensor fusion
    DisposableEffect(Unit) {
        onDispose { sensorFusion.stop() }
    }

    // Show scroll popup overlay when requested
    if (showScrollPopup) {
        ScrollPopupScreen(
            parentScreen = "mouse",
            onBack = { showScrollPopup = false }
        )
        return
    }

    Scaffold {
        TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Title with long-press to open scroll popup
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showScrollPopup = true
                            }
                        )
                    }
            ) {
                Text(
                    text = "WearMouse • Point to move • Tap to click",
                    fontSize = 11.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center
                )
            }

            // Error feedback
            if (connectionError != null) {
                Text(
                    text = "Connection Error: $connectionError",
                    color = Color.Red,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                )
            }

            // Main click area - ONLY FOR CLICKS, NOT MOVEMENT
            Box(
                modifier = Modifier
                    .size(mouseAreaSize)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (isConnected) {
                                    lastAction = "Left Click"
                                    actionFeedbackVisible = true
                                    try {
                                        HidClient.mouseLeftClick()
                                    } catch (e: Exception) {
                                        Log.e("MouseScreen", "Left click error: ${e.message}")
                                    }
                                }
                            },
                            onLongPress = {
                                if (isConnected) {
                                    lastAction = "Right Click"
                                    actionFeedbackVisible = true
                                    try {
                                        HidClient.mouseRightClick()
                                    } catch (e: Exception) {
                                        Log.e("MouseScreen", "Right click error: ${e.message}")
                                    }
                                }
                            },
                            onDoubleTap = {
                                if (isConnected) {
                                    lastAction = "Double Click"
                                    actionFeedbackVisible = true
                                    try {
                                        HidClient.mouseDoubleClick()
                                    } catch (e: Exception) {
                                        Log.e("MouseScreen", "Double click error: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                    .background(
                        color = if (isConnected) Color(0xFF404040) else Color(0xFF202020),
                        shape = CircleShape
                    )
            ) {
                // Visual indicators
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Outer circle
                    drawCircle(
                        color = Color(0xFF404040),
                        radius = radius - 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Crosshairs
                    val crosshairColor = if (isConnected) Color(0xFF666666) else Color(0xFF333333)
                    val crosshairLength = 15.dp.toPx()

                    drawLine(
                        color = crosshairColor,
                        start = Offset(center.x - crosshairLength, center.y),
                        end = Offset(center.x + crosshairLength, center.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    drawLine(
                        color = crosshairColor,
                        start = Offset(center.x, center.y - crosshairLength),
                        end = Offset(center.x, center.y + crosshairLength),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Center dot
                    drawCircle(
                        color = if (isConnected) Color(0xFF2196F3) else Color(0xFF666666),
                        radius = 3.dp.toPx(),
                        center = center
                    )
                }
            }

            // Action feedback overlay
            if (actionFeedbackVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lastAction,
                        fontSize = 12.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Connection status indicator
            if (!isConnected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(mouseAreaSize)
                        .clip(CircleShape)
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠",
                            fontSize = 24.sp,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = "Not Connected",
                            fontSize = 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Instructions at bottom
            Text(
                text = "Point to move • Tap to click • Long press for right click",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                fontSize = 8.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }
    }

    LaunchedEffect(Unit) {
        HidClient.currentDeviceProfile = DeviceProfile.Mouse
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun MouseScreenPreview() {
    MouseScreen()
}
