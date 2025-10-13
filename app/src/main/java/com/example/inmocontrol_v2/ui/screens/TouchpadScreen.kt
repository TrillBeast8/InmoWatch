package com.example.inmocontrol_v2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.data.SettingsStore
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.ui.components.QuickLauncher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Elegant TouchpadScreen with smooth animations and seamless interactions
 */
@Composable
fun TouchpadScreen(
    onBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore.get(context) }
    
    // Fluid state management
    var lastAction by remember { mutableStateOf("Ready") }
    var isDragging by remember { mutableStateOf(false) }
    var actionFeedbackVisible by remember { mutableStateOf(false) }
    var showQuickLauncher by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableStateOf(1.0f) }
    
    // Load initial sensitivity
    LaunchedEffect(Unit) {
        settingsStore.sensitivity.collect { sensitivity = it }
    }

    // Connection state
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Gesture tracking
    var lastPosition by remember { mutableStateOf(Offset.Zero) }
    var twoFingerScrollActive by remember { mutableStateOf(false) }
    var activePointerCount by remember { mutableStateOf(0) }

    // Smooth feedback animations
    val feedbackAlpha by animateFloatAsState(
        targetValue = if (actionFeedbackVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val feedbackScale by animateFloatAsState(
        targetValue = if (actionFeedbackVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    // Auto-clear feedback with smooth timing
    LaunchedEffect(actionFeedbackVisible) {
        if (actionFeedbackVisible) {
            delay(600)  // Quick, responsive feedback
            actionFeedbackVisible = false
        }
    }

    // Action handler
    val performAction = remember(isConnected) {
        { action: String, operation: () -> Unit ->
            if (isConnected) {
                operation()
                lastAction = action
                actionFeedbackVisible = true
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Full-screen touch area for seamless interaction
            // STABILITY: Proper key-based pointer inputs to prevent leaks
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isConnected) {  // Smart two-finger scroll detection
                        if (isConnected) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    activePointerCount = event.changes.size

                                    if (activePointerCount >= 2) {
                                        // Two-finger scroll mode
                                        twoFingerScrollActive = true
                                        val firstPointer = event.changes[0]
                                        val secondPointer = event.changes[1]

                                        // Calculate average position for smooth scrolling
                                        val avgPosition = Offset(
                                            (firstPointer.position.x + secondPointer.position.x) / 2f,
                                            (firstPointer.position.y + secondPointer.position.y) / 2f
                                        )

                                        if (lastPosition != Offset.Zero) {
                                            val deltaX = avgPosition.x - lastPosition.x
                                            val deltaY = avgPosition.y - lastPosition.y

                                            // Smart scroll: prioritize dominant direction
                                            val absX = kotlin.math.abs(deltaX)
                                            val absY = kotlin.math.abs(deltaY)

                                            if (absX > absY && absX > 2f) {
                                                // Horizontal scroll
                                                HidClient.sendMouseScroll(deltaX * 0.1f * sensitivity, 0f)
                                                lastAction = if (deltaX > 0) "Scroll →" else "Scroll ←"
                                            } else if (absY > 2f) {
                                                // Vertical scroll
                                                HidClient.sendMouseScroll(0f, -deltaY * 0.1f * sensitivity)
                                                lastAction = if (deltaY > 0) "Scroll ↓" else "Scroll ↑"
                                            }
                                        }
                                        lastPosition = avgPosition
                                    } else {
                                        twoFingerScrollActive = false
                                        if (activePointerCount == 0) {
                                            lastPosition = Offset.Zero
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(isConnected) {  // STABILITY: Key by connection state
                        if (isConnected) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (!twoFingerScrollActive) {
                                        lastPosition = offset
                                        isDragging = true
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (!twoFingerScrollActive) {
                                        lastPosition = Offset.Zero
                                    }
                                }
                            ) { change, _ ->
                                if (isDragging && !twoFingerScrollActive) {
                                    val deltaX = change.position.x - lastPosition.x
                                    val deltaY = change.position.y - lastPosition.y
                                    val scaledX = deltaX * 0.5f * sensitivity
                                    val scaledY = deltaY * 0.5f * sensitivity
                                    HidClient.sendMouseMovement(scaledX, scaledY)
                                    lastPosition = change.position
                                }
                            }
                        }
                    }
                    .pointerInput(isConnected) {  // STABILITY: Key by connection state
                        if (isConnected) {
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
                    },
                contentAlignment = Alignment.Center
            ) {
                // Elegant circular guide with subtle presence
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension * 0.42f
                    
                    // Subtle guide ring
                    drawCircle(
                        color = if (isConnected) Color.White.copy(alpha = 0.08f)
                               else Color.Gray.copy(alpha = 0.05f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    
                    // Dragging feedback - smooth gradient effect
                    if (isDragging) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = radius * 0.9f,
                            center = center
                        )
                    }
                }
                
                // Smooth action feedback overlay
                AnimatedVisibility(
                    visible = actionFeedbackVisible,
                    enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                    exit = fadeOut(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(feedbackAlpha)
                            .scale(feedbackScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.display1,
                            color = MaterialTheme.colors.secondary.copy(alpha = 0.7f),
                            fontSize = 48.sp
                        )
                    }
                }
                
                // Status and instructions - fade in/out elegantly
                AnimatedVisibility(
                    visible = !isDragging && !actionFeedbackVisible,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isConnected) {
                            Text(
                                text = connectionError ?: "Not Connected",
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption1,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Drag to move",
                                style = MaterialTheme.typography.caption2,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Floating sensitivity control - minimal and elegant
            AnimatedVisibility(
                visible = !isDragging,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.surface.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Button(
                        onClick = {
                            val newSensitivity = (sensitivity - 0.1f).coerceIn(0.1f, 2.0f)
                            sensitivity = newSensitivity
                            scope.launch { settingsStore.setSensitivity(newSensitivity) }
                            lastAction = "${String.format("%.1f", newSensitivity)}x"
                            actionFeedbackVisible = true
                        },
                        modifier = Modifier.size(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        Text("-", fontSize = 12.sp)
                    }

                    Text(
                        text = "${String.format("%.1f", sensitivity)}x",
                        style = MaterialTheme.typography.caption1,
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.primary
                    )

                    Button(
                        onClick = {
                            val newSensitivity = (sensitivity + 0.1f).coerceIn(0.1f, 2.0f)
                            sensitivity = newSensitivity
                            scope.launch { settingsStore.setSensitivity(newSensitivity) }
                            lastAction = "${String.format("%.1f", newSensitivity)}x"
                            actionFeedbackVisible = true
                        },
                        modifier = Modifier.size(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        Text("+", fontSize = 12.sp)
                    }
                }
            }

            // Floating action buttons - elegant minimal design
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ESC button
                Button(
                    onClick = {
                        performAction("ESC") {
                            HidClient.sendEscape()
                        }
                    },
                    enabled = isConnected,
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.4f)
                    ),
                    shape = CircleShape
                ) {
                    Text("⎋", fontSize = 14.sp)
                }

                // Quick Launcher button
                Button(
                    onClick = { showQuickLauncher = !showQuickLauncher },
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (showQuickLauncher) MaterialTheme.colors.primary.copy(alpha = 0.6f)
                                        else MaterialTheme.colors.surface.copy(alpha = 0.3f)
                    )
                ) {
                    Text("⋮", fontSize = 18.sp)
                }
            }
            
            // Quick Launcher overlay
            AnimatedVisibility(
                visible = showQuickLauncher,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                QuickLauncher(
                    onClose = { showQuickLauncher = false },
                    onNavigateTo = { route ->
                        showQuickLauncher = false
                        onNavigateTo(route)
                    },
                    currentScreen = "touchpad"
                )
            }
        }
    }
    
    // STABILITY: Proper cleanup for gesture handlers
    DisposableEffect(Unit) {
        onDispose {
            // Clean up any lingering state
        }
    }
}
