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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Text as WearText
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.DeviceProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun TouchpadScreen(
    mode: String = "touchpad",
    onOpenScrollPopup: () -> Unit = {}
) {
    LaunchedEffect(mode) {
        HidClient.currentDeviceProfile = when (mode) {
            "touchpad" -> DeviceProfile.Mouse
            else -> DeviceProfile.Mouse // fallback
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val lastAction = remember { mutableStateOf("Ready") }
    val isDragging = remember { mutableStateOf(false) }
    val actionFeedbackVisible = remember { mutableStateOf(false) }
    var showScrollPopup by remember { mutableStateOf(false) }

    // Gesture tracking
    val tapCount = remember { mutableStateOf(0) }
    val lastTapTime = remember { mutableStateOf(0L) }
    val isLongPressing = remember { mutableStateOf(false) }
    val isDragOperation = remember { mutableStateOf(false) }
    val totalDragDistance = remember { mutableStateOf(0f) }

    val configuration = LocalConfiguration.current
    val minDimension = min(configuration.screenWidthDp.dp.value, configuration.screenHeightDp.dp.value)
    val touchpadSize = (minDimension * 0.75f).dp

    // Timing constants
    val doubleTapWindow = 400L
    val longPressDelay = 500L
    val dragThreshold = 20f

    // Sensitivity
    val movementSensitivity = 2.5f

    // Auto-hide feedback
    LaunchedEffect(actionFeedbackVisible.value) {
        if (actionFeedbackVisible.value) {
            delay(2000)
            actionFeedbackVisible.value = false
            if (!isDragging.value) {
                lastAction.value = "Ready"
            }
        }
    }

    // Show scroll popup overlay when requested
    if (showScrollPopup) {
        ScrollPopupScreen(
            parentScreen = "touchpad",
            onBack = { showScrollPopup = false }
        )
        return
    }

    Scaffold {
        TimeText()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title with long-press to open scroll popup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showScrollPopup = true
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                WearText(
                    text = "Touchpad • Hold for scroll",
                    fontSize = 14.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center
                )
            }

            // Enhanced touchpad with unified gesture handling
            Box(
                modifier = Modifier
                    .size(touchpadSize)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .pointerInput(Unit) {
                        var isPressed = false
                        var longPressTriggered = false
                        var lastPosition = Offset.Zero

                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                when (event.type) {
                                    PointerEventType.Press -> {
                                        val pointer = event.changes.first()
                                        isPressed = true
                                        longPressTriggered = false
                                        lastPosition = pointer.position
                                        totalDragDistance.value = 0f

                                        // Start long press detection
                                        coroutineScope.launch {
                                            delay(longPressDelay)
                                            if (isPressed && !longPressTriggered && totalDragDistance.value < dragThreshold) {
                                                longPressTriggered = true
                                                isLongPressing.value = true
                                                lastAction.value = "Enter/OK"
                                                actionFeedbackVisible.value = true
                                                try {
                                                    HidClient.sendInmoConfirm()
                                                } catch (e: Exception) {
                                                    Log.e("TouchpadScreen", "Enter error: ${e.message}")
                                                }
                                            }
                                        }
                                        pointer.consume()
                                    }

                                    PointerEventType.Move -> {
                                        if (isPressed) {
                                            val pointer = event.changes.first()
                                            val currentPosition = pointer.position
                                            val delta = currentPosition - lastPosition

                                            // Track total movement for drag threshold
                                            totalDragDistance.value += sqrt(delta.x * delta.x + delta.y * delta.y)

                                            if (totalDragDistance.value > dragThreshold) {
                                                if (longPressTriggered && !isDragOperation.value) {
                                                    // Start drag operation
                                                    isDragOperation.value = true
                                                    isDragging.value = true
                                                    lastAction.value = "Dragging"
                                                    actionFeedbackVisible.value = true
                                                    try {
                                                        HidClient.mouseDragMove(0, 0)
                                                    } catch (e: Exception) {
                                                        Log.e("TouchpadScreen", "Drag start error: ${e.message}")
                                                    }
                                                }

                                                // Handle movement/dragging
                                                try {
                                                    val scaledX = (delta.x * movementSensitivity).toInt()
                                                    val scaledY = (delta.y * movementSensitivity).toInt()

                                                    if (isDragOperation.value) {
                                                        // Continue drag operation
                                                        HidClient.mouseDragMove(scaledX, scaledY)
                                                    } else {
                                                        // Normal cursor movement
                                                        HidClient.moveMouse(scaledX, scaledY)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("TouchpadScreen", "Movement error: ${e.message}")
                                                }
                                            }

                                            lastPosition = currentPosition
                                            pointer.consume()
                                        }
                                    }

                                    PointerEventType.Release -> {
                                        val pointer = event.changes.first()
                                        val currentTime = System.currentTimeMillis()

                                        if (isPressed) {
                                            if (!longPressTriggered && totalDragDistance.value < dragThreshold) {
                                                // Handle tap gestures
                                                val timeSinceLastTap = currentTime - lastTapTime.value

                                                if (timeSinceLastTap < doubleTapWindow && tapCount.value == 1) {
                                                    // Double tap
                                                    tapCount.value = 0
                                                    lastAction.value = "Double Tap"
                                                    actionFeedbackVisible.value = true
                                                    try {
                                                        HidClient.mouseDoubleClick()
                                                    } catch (e: Exception) {
                                                        Log.e("TouchpadScreen", "Double tap error: ${e.message}")
                                                    }
                                                } else {
                                                    // First tap
                                                    tapCount.value = 1
                                                    lastTapTime.value = currentTime

                                                    // Wait to see if second tap comes
                                                    coroutineScope.launch {
                                                        delay(doubleTapWindow)
                                                        if (tapCount.value == 1) {
                                                            // Single tap
                                                            tapCount.value = 0
                                                            lastAction.value = "Left Click"
                                                            actionFeedbackVisible.value = true
                                                            try {
                                                                HidClient.mouseLeftClick()
                                                            } catch (e: Exception) {
                                                                Log.e("TouchpadScreen", "Left click error: ${e.message}")
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (isDragOperation.value) {
                                                // End drag operation
                                                isDragOperation.value = false
                                                isDragging.value = false
                                                lastAction.value = "Drag End"
                                                actionFeedbackVisible.value = true
                                                try {
                                                    HidClient.mouseDragEnd()
                                                } catch (e: Exception) {
                                                    Log.e("TouchpadScreen", "Drag end error: ${e.message}")
                                                }
                                            }

                                            isPressed = false
                                            longPressTriggered = false
                                            isLongPressing.value = false
                                            totalDragDistance.value = 0f
                                        }
                                        pointer.consume()
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Visual feedback
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Outer border
                    drawCircle(
                        color = Color(0xFF404040),
                        radius = radius - 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Center dot
                    drawCircle(
                        color = Color(0xFF606060),
                        radius = 4.dp.toPx(),
                        center = center
                    )

                    // Drag indicator
                    if (isDragging.value) {
                        drawCircle(
                            color = Color(0xFF00FF00).copy(alpha = 0.3f),
                            radius = radius - 10.dp.toPx(),
                            center = center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status and action feedback
            if (actionFeedbackVisible.value) {
                WearText(
                    text = lastAction.value,
                    fontSize = 12.sp,
                    color = when {
                        isDragging.value -> Color(0xFF00FF00)
                        isLongPressing.value -> Color(0xFFFFAA00)
                        else -> Color(0xFFCCCCCC)
                    },
                    textAlign = TextAlign.Center
                )
            }

            // Instructions
            WearText(
                text = "Tap: Click • Double tap: Double click • Hold: Enter/OK • Hold + Move: Drag",
                fontSize = 8.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// @Preview(device = "id:wearos_small_round", showSystemUi = true)
// @Composable
// fun TouchpadScreenPreview() {
//     TouchpadScreen()
// }
