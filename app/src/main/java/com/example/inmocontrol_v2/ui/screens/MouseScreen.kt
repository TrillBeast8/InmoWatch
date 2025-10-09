package com.example.inmocontrol_v2.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.sensors.WearMouseSensorFusion
import com.example.inmocontrol_v2.ui.gestures.detectTwoFingerSwipe
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * MouseScreen - Preserving original UI and features exactly as designed
 */
@Composable
fun MouseScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    val context = LocalContext.current

    // State management - preserving original design
    var lastAction by remember { mutableStateOf("Ready") }
    var actionFeedbackVisible by remember { mutableStateOf(false) }

    // Screen size calculation - preserving original logic
    val configuration = LocalConfiguration.current
    val mouseAreaSize = remember(configuration) {
        val minDimension = min(configuration.screenWidthDp, configuration.screenHeightDp)
        (minDimension * 0.85f).dp
    }

    // Sensor fusion - preserving original implementation
    val sensorFusion = remember { WearMouseSensorFusion(context) }

    // Connection state - preserving original reactive design
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Feedback timing - preserving original timing
    LaunchedEffect(actionFeedbackVisible) {
        if (actionFeedbackVisible) {
            delay(1500)
            actionFeedbackVisible = false
        }
    }

    // Sensor lifecycle - preserving original management
    DisposableEffect(isConnected) {
        if (isConnected) {
            sensorFusion.start { movement ->
                HidClient.sendMouseMovement(movement.deltaX, movement.deltaY)
            }
        } else {
            sensorFusion.stop()
        }

        onDispose {
            sensorFusion.stop()
        }
    }

    // Click handler - preserving original logic
    val handleClick = remember {
        { action: String, clickType: () -> Unit ->
            if (isConnected) {
                clickType()
                lastAction = action
                actionFeedbackVisible = true
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier.detectTwoFingerSwipe(
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Title button - preserving original design and functionality
            Button(
                onClick = onScrollPopup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
            ) {
                Text(
                    text = "Mouse",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary
                )
            }

            // Connection status - preserving original error display
            if (!isConnected) {
                Text(
                    text = connectionError ?: "Not connected",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Mouse area - preserving original circular design and gestures
            Box(
                modifier = Modifier
                    .size(mouseAreaSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.1f))
                    .pointerInput(isConnected) {
                        if (isConnected) {
                            detectTapGestures(
                                onTap = {
                                    handleClick("Left Click") { HidClient.sendLeftClick() }
                                },
                                onLongPress = {
                                    handleClick("Right Click") { HidClient.sendRightClick() }
                                },
                                onDoubleTap = {
                                    handleClick("Double Click") { HidClient.sendDoubleClick() }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Visual feedback canvas - preserving original graphics
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2

                    // Outer circle - preserving original styling
                    drawCircle(
                        color = if (isConnected) Color.White.copy(alpha = 0.3f)
                               else Color.Gray.copy(alpha = 0.2f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Inner feedback circle - preserving original animation
                    if (actionFeedbackVisible) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = radius * 0.7f,
                            center = center
                        )
                    }
                }

                // Instruction text - preserving original layout and content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isConnected) "Tap: Left Click" else "Not Connected",
                        style = MaterialTheme.typography.caption2,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    if (isConnected) {
                        Text(
                            text = "Hold: Right Click",
                            style = MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Double: Double Click",
                            style = MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Action feedback - preserving original feedback design
            if (actionFeedbackVisible && isConnected) {
                Text(
                    text = "✓ $lastAction",
                    color = MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.caption1,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Control buttons - preserving original button layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        handleClick("Middle Click") { HidClient.sendMiddleClick() }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text("Mid", fontSize = 10.sp)
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back", fontSize = 10.sp)
                }
            }
        }
    }
}
