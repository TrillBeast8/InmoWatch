package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.DeviceProfile
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Optimized TouchpadScreen with improved performance and reduced memory usage
 */
@Composable
fun TouchpadScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {}
) {
    // Set device profile once
    LaunchedEffect(Unit) {
        HidClient.currentDeviceProfile = DeviceProfile.Mouse
    }

    // Optimized state management with reduced recompositions
    var lastAction by remember { mutableStateOf("Ready") }
    var isDragging by remember { mutableStateOf(false) }
    var actionFeedbackVisible by remember { mutableStateOf(false) }

    // Cached configuration values
    val configuration = LocalConfiguration.current
    val touchpadSize = remember(configuration) {
        val minDimension = min(configuration.screenWidthDp, configuration.screenHeightDp)
        (minDimension * 0.85f).dp
    }

    // Connection state
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Optimized gesture tracking with cached values
    var lastPosition by remember { mutableStateOf(Offset.Zero) }

    // Optimized feedback timing
    LaunchedEffect(actionFeedbackVisible) {
        if (actionFeedbackVisible) {
            delay(1500)
            actionFeedbackVisible = false
        }
    }

    // Optimized action handler
    val performAction = remember {
        { action: String, operation: () -> Unit ->
            if (isConnected) {
                operation()
                lastAction = action
                actionFeedbackVisible = true
            }
        }
    }

    // Optimized gesture detection
    val gestureModifier = remember(isConnected) {
        if (isConnected) {
            Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        lastPosition = offset
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        lastPosition = Offset.Zero
                    }
                ) { change, _ ->
                    if (isDragging) {
                        val deltaX = change.position.x - lastPosition.x
                        val deltaY = change.position.y - lastPosition.y

                        // Scale movement for touchpad feel
                        val scaledX = deltaX * 0.5f
                        val scaledY = deltaY * 0.5f

                        HidClient.sendMouseMovement(scaledX, scaledY)
                        lastPosition = change.position
                    }
                }
            }.pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        performAction("Left Click") { HidClient.sendLeftClick() }
                    },
                    onLongPress = {
                        performAction("Right Click") { HidClient.sendRightClick() }
                    },
                    onDoubleTap = {
                        performAction("Double Click") { HidClient.sendDoubleClick() }
                    }
                )
            }
        } else {
            Modifier
        }
    }

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Optimized title with click-to-scroll
            Button(
                onClick = onScrollPopup,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.wear.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent
                )
            ) {
                WearText(
                    text = "Touchpad",
                    style = androidx.wear.compose.material.MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    color = androidx.wear.compose.material.MaterialTheme.colors.primary
                )
            }

            // Connection status - only show when not connected
            if (!isConnected) {
                WearText(
                    text = connectionError ?: "Not connected",
                    color = androidx.wear.compose.material.MaterialTheme.colors.error,
                    style = androidx.wear.compose.material.MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Optimized touchpad area
            Box(
                modifier = Modifier
                    .size(touchpadSize)
                    .clip(CircleShape)
                    .background(
                        androidx.wear.compose.material.MaterialTheme.colors.surface.copy(alpha = 0.1f)
                    )
                    .then(gestureModifier),
                contentAlignment = Alignment.Center
            ) {
                // Optimized visual feedback
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2

                    // Draw outer circle
                    drawCircle(
                        color = if (isConnected) Color.White.copy(alpha = 0.3f)
                        else Color.Gray.copy(alpha = 0.2f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw active feedback
                    if (isDragging) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = radius * 0.7f,
                            center = center
                        )
                    }

                    if (actionFeedbackVisible) {
                        drawCircle(
                            color = Color.Blue.copy(alpha = 0.3f),
                            radius = radius * 0.5f,
                            center = center
                        )
                    }
                }

                // Centered instruction text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WearText(
                        text = if (isConnected) "Drag: Move cursor" else "Not Connected",
                        style = androidx.wear.compose.material.MaterialTheme.typography.caption2,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    if (isConnected) {
                        WearText(
                            text = "Tap: Left Click",
                            style = androidx.wear.compose.material.MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        WearText(
                            text = "Hold: Right Click",
                            style = androidx.wear.compose.material.MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Optimized action feedback
            if (actionFeedbackVisible && isConnected) {
                WearText(
                    text = "✓ $lastAction",
                    color = androidx.wear.compose.material.MaterialTheme.colors.secondary,
                    style = androidx.wear.compose.material.MaterialTheme.typography.caption1,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Optimized control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        performAction("Middle Click") { HidClient.sendMiddleClick() }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    colors = androidx.wear.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = androidx.wear.compose.material.MaterialTheme.colors.primary
                    )
                ) {
                    WearText("Mid", fontSize = 10.sp)
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    WearText("Back", fontSize = 10.sp)
                }
            }
        }
    }
}

// @Preview(device = "id:wearos_small_round", showSystemUi = true)
// @Composable
// fun TouchpadScreenPreview() {
//     TouchpadScreen()
// }
