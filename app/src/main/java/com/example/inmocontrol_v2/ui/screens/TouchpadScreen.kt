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

                                // Check for multi-touch (two-finger scrolling)
                                isMultiTouch = changes.size >= 2

                                if (isMultiTouch) {
                                    // Two-finger scrolling
                                    if (changes.size >= 2) {
                                        val change1 = changes[0]
                                        val change2 = changes[1]

                                        if (change1.pressed && change2.pressed) {
                                            val avgDelta = Offset(
                                                (change1.position.x - change1.previousPosition.x +
                                                 change2.position.x - change2.previousPosition.x) / 2f,
                                                (change1.position.y - change1.previousPosition.y +
                                                 change2.position.y - change2.previousPosition.y) / 2f
                                            )

                                            if (abs(avgDelta.x) > 10 || abs(avgDelta.y) > 10) {
                                                coroutineScope.launch {
                                                    lastAction.value = "Scrolling"
                                                    actionFeedbackVisible.value = true
                                                    try {
                                                        HidClient.mouseScroll(
                                                            (avgDelta.x * scrollSensitivity).toInt(),
                                                            (avgDelta.y * scrollSensitivity).toInt()
                                                        )
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        }
                                        change1.consume()
                                        change2.consume()
                                    }
                                } else if (changes.size == 1) {
                                    // Single finger - mouse movement or drag
                                    val change = changes[0]

                                    if (change.pressed) {
                                        if (!dragStarted) {
                                            // Start drag
                                            isDragging.value = true
                                            dragStarted = true
                                            coroutineScope.launch {
                                                try {
                                                    HidClient.mouseDragMove(0, 0) // Start drag mode
                                                } catch (e: Exception) {}
                                            }
                                        }

                                        // Continue drag movement
                                        val deltaX = change.position.x - change.previousPosition.x
                                        val deltaY = change.position.y - change.previousPosition.y

                                        if (abs(deltaX) > 5 || abs(deltaY) > 5) {
                                            coroutineScope.launch {
                                                lastAction.value = "Dragging"
                                                actionFeedbackVisible.value = true
                                                try {
                                                    HidClient.mouseDragMove(deltaX.toInt(), deltaY.toInt())
                                                } catch (e: Exception) {}
                                            }
                                        }
                                        change.consume()
                                    }
                                }
                            } while (changes.any { it.pressed })

                            // End drag when all pointers are released
                            if (dragStarted) {
                                isDragging.value = false
                                coroutineScope.launch {
                                    try {
                                        HidClient.mouseDragEnd()
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                coroutineScope.launch {
                                    lastAction.value = "Left Click"
                                    actionFeedbackVisible.value = true
                                    try {
                                        HidClient.mouseLeftClick()
                                    } catch (e: Exception) {}
                                }
                            },
                            onLongPress = {
                                coroutineScope.launch {
                                    lastAction.value = "Right Click"
                                    actionFeedbackVisible.value = true
                                    try {
                                        HidClient.mouseRightClick()
                                    } catch (e: Exception) {}
                                }
                            },
                            onDoubleTap = {
                                coroutineScope.launch {
                                    lastAction.value = "Double Click"
                                    actionFeedbackVisible.value = true
                                    try {
                                        HidClient.mouseDoubleClick()
                                    } catch (e: Exception) {}
                                }
                            }
                        )
                    }
            ) {
                // Visual feedback for the touchpad area
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Outer ring - touchpad boundary
                    drawCircle(
                        color = if (isDragging.value) Color.Cyan.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.4f),
                        radius = radius - 4.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Grid lines for reference
                    for (i in 1..3) {
                        val lineOffset = (radius / 2f) * i / 2f
                        // Vertical lines
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(center.x - lineOffset, center.y - radius + 10.dp.toPx()),
                            end = Offset(center.x - lineOffset, center.y + radius - 10.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(center.x + lineOffset, center.y - radius + 10.dp.toPx()),
                            end = Offset(center.x + lineOffset, center.y + radius - 10.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Horizontal lines
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(center.x - radius + 10.dp.toPx(), center.y - lineOffset),
                            end = Offset(center.x + radius - 10.dp.toPx(), center.y - lineOffset),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(center.x - radius + 10.dp.toPx(), center.y + lineOffset),
                            end = Offset(center.x + radius - 10.dp.toPx(), center.y + lineOffset),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // Center indicator showing current action
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDragging.value -> Color.Cyan.copy(alpha = 0.8f)
                            actionFeedbackVisible.value -> Color.Green.copy(alpha = 0.8f)
                            else -> Color.Blue.copy(alpha = 0.6f)
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

            // Instructions at bottom
            WearText(
                text = "1 finger: move • 2 fingers: scroll",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun TouchpadScreenPreview() {
    TouchpadScreen()
}
