package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text as WearText
import androidx.wear.compose.material.TimeText
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag

@Composable
fun TouchpadScreen() {
    val coroutineScope = rememberCoroutineScope()
    val lastAction = remember { mutableStateOf("Tap to start") }
    val isDragging = remember { mutableStateOf(false) }
    val actionFeedbackVisible = remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { com.example.inmocontrol_v2.data.SettingsStore.get(context) }
    val scrollSensitivity = settingsStore.scrollSensitivity.collectAsState(initial = 1.0f).value

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Calculate sizes based on screen dimensions
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val minDimension = min(screenWidthDp.value, screenHeightDp.value)

    // Touchpad takes most of the screen
    val touchpadSize = (minDimension * 0.9f).dp
    // Center indicator is smaller
    val indicatorSize = (minDimension * 0.25f).dp

    // Auto-hide action feedback after 2 seconds
    LaunchedEffect(actionFeedbackVisible.value) {
        if (actionFeedbackVisible.value) {
            delay(2000)
            actionFeedbackVisible.value = false
        }
    }

    androidx.wear.compose.material.Scaffold {
        TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main touchpad area - full interaction surface with two-finger scrolling
            Box(
                modifier = Modifier
                    .size(touchpadSize)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            var isMultiTouch = false
                            var dragStarted = false

                            do {
                                val event = awaitPointerEvent()
                                val changes = event.changes

                                // Detect if we have two fingers
                                if (changes.size >= 2 && !isMultiTouch) {
                                    isMultiTouch = true
                                    coroutineScope.launch {
                                        lastAction.value = "Two-finger mode"
                                        actionFeedbackVisible.value = true
                                    }
                                }

                                if (isMultiTouch && changes.size >= 2) {
                                    // Two-finger scrolling
                                    val change1 = changes[0]
                                    val change2 = changes[1]

                                    // Calculate average movement of both fingers
                                    val avgDragAmount = Offset(
                                        (change1.position.x - change1.previousPosition.x +
                                         change2.position.x - change2.previousPosition.x) / 2f,
                                        (change1.position.y - change1.previousPosition.y +
                                         change2.position.y - change2.previousPosition.y) / 2f
                                    )

                                    if (abs(avgDragAmount.x) > 5f || abs(avgDragAmount.y) > 5f) {
                                        coroutineScope.launch {
                                            if (abs(avgDragAmount.y) > abs(avgDragAmount.x)) {
                                                // Vertical scroll - up/down
                                                val scaledY = (avgDragAmount.y * scrollSensitivity * 2).toInt()
                                                val direction = if (scaledY < 0) "2F Scroll ↑" else "2F Scroll ↓"
                                                lastAction.value = direction
                                                actionFeedbackVisible.value = true
                                                try {
                                                    HidClient.instance()?.mouseScroll(0, scaledY)
                                                } catch (e: Exception) {}
                                            } else {
                                                // Horizontal scroll - left/right
                                                val scaledX = (avgDragAmount.x * scrollSensitivity * 2).toInt()
                                                val direction = if (scaledX < 0) "2F Scroll ←" else "2F Scroll →"
                                                lastAction.value = direction
                                                actionFeedbackVisible.value = true
                                                try {
                                                    HidClient.instance()?.mouseScroll(scaledX, 0)
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    }

                                    change1.consume()
                                    change2.consume()
                                } else if (!isMultiTouch && changes.size == 1) {
                                    // Single finger - handle mouse movement and taps
                                    val change = changes[0]

                                    if (!dragStarted && change.pressed) {
                                        val dragAmount = change.position - change.previousPosition
                                        if (abs(dragAmount.x) > 10f || abs(dragAmount.y) > 10f) {
                                            dragStarted = true
                                            isDragging.value = true
                                            coroutineScope.launch {
                                                lastAction.value = "Moving..."
                                                actionFeedbackVisible.value = true
                                                try {
                                                    HidClient.instance()?.moveMouse(dragAmount.x.toInt(), dragAmount.y.toInt())
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    } else if (dragStarted && change.pressed) {
                                        val dragAmount = change.position - change.previousPosition
                                        coroutineScope.launch {
                                            try {
                                                HidClient.instance()?.moveMouse(dragAmount.x.toInt(), dragAmount.y.toInt())
                                            } catch (e: Exception) {}
                                        }
                                    }

                                    change.consume()
                                }

                            } while (changes.any { it.pressed })

                            // Handle end of gesture
                            if (dragStarted) {
                                isDragging.value = false
                                try {
                                    HidClient.instance()?.mouseDragEnd()
                                } catch (e: Exception) {}
                            } else if (!isMultiTouch) {
                                // This was a simple tap
                                coroutineScope.launch {
                                    lastAction.value = "Left Click"
                                    actionFeedbackVisible.value = true
                                    try {
                                        HidClient.instance()?.mouseLeftClick()
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        // Handle long press for right click and double tap
                        detectTapGestures(
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
                // Visual feedback ring around the touchpad
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Outer ring - touchpad boundary
                    drawCircle(
                        color = if (isDragging.value) Color.Blue.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.4f),
                        radius = radius - 4.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Center indicator circle showing last action
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDragging.value -> Color.Blue.copy(alpha = 0.8f)
                            actionFeedbackVisible.value -> Color.Green.copy(alpha = 0.8f)
                            else -> Color.Gray.copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                WearText(
                    text = lastAction.value,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.padding(4.dp),
                    maxLines = 2
                )
            }

            // Instruction text at the top
            WearText(
                text = "Touchpad",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            // Sensitivity indicator at bottom
            WearText(
                text = "Scroll: ${scrollSensitivity}x",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }

    // Set input mode to TOUCHPAD when screen loads
    LaunchedEffect(Unit) {
        HidClient.instance()?.setInputMode(com.example.inmocontrol_v2.hid.HidService.InputMode.TOUCHPAD)
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun TouchpadScreenPreview() {
    TouchpadScreen()
}
