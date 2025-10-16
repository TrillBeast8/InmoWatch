package com.example.inmocontrol_v2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.data.SettingsStore
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.sensors.WearMouseSensorFusion
import com.example.inmocontrol_v2.ui.components.QuickLauncher
import kotlinx.coroutines.delay

/**
 * MouseScreen - Smooth, elegant, fluid UI for circular Wear OS
 * Seamless interactions with instant response and modern polish
 */
@Composable
fun MouseScreen(
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }

    // State with smooth animations
    var lastAction by remember { mutableStateOf("") }
    var actionFeedbackVisible by remember { mutableStateOf(false) }
    var showQuickLauncher by remember { mutableStateOf(false) }
    val sensitivity by settingsStore.sensitivity.collectAsState(initial = 1.0f)

    // Sensor fusion
    val sensorFusion = remember { WearMouseSensorFusion(context) }

    // Connection state
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Smooth feedback animation
    val feedbackAlpha by animateFloatAsState(
        targetValue = if (actionFeedbackVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "feedbackAlpha"
    )

    val feedbackScale by animateFloatAsState(
        targetValue = if (actionFeedbackVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "feedbackScale"
    )

    // Quick fade out
    LaunchedEffect(actionFeedbackVisible) {
        if (actionFeedbackVisible) {
            delay(600)
            actionFeedbackVisible = false
        }
    }

    // Sensor lifecycle with sensitivity - STABILITY: Proper cleanup
    DisposableEffect(isConnected) {
        if (isConnected) {
            sensorFusion.start { movement ->
                val scaledX = movement.deltaX * sensitivity
                val scaledY = movement.deltaY * sensitivity
                HidClient.sendMouseMovement(scaledX, scaledY)
            }
        } else {
            sensorFusion.stop()
        }

        onDispose {
            sensorFusion.stop()
        }
    }

    // Smooth click handler
    val handleClick = remember(isConnected) {
        { action: String, clickType: () -> Unit ->
            if (isConnected) {
                clickType()
                lastAction = action
                actionFeedbackVisible = true
            }
        }
    }

    // Disable predictive back gesture to prevent accidental navigation during mouse movements
    androidx.activity.compose.PredictiveBackHandler(enabled = false) { }

    Scaffold(
        timeText = {
            AnimatedVisibility(
                visible = !isConnected,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TimeText()
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isConnected) {
                    if (isConnected) {
                        detectTapGestures(
                            onTap = { handleClick("Left", HidClient::sendLeftClick) },
                            onLongPress = { handleClick("Right", HidClient::sendRightClick) },
                            onDoubleTap = { handleClick("Double", HidClient::sendDoubleClick) }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Subtle circular guide - barely visible, elegant
            Canvas(modifier = Modifier.fillMaxSize(0.92f)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2

                // Outer ring - subtle
                drawCircle(
                    color = if (isConnected) Color.White.copy(alpha = 0.08f)
                           else Color.Gray.copy(alpha = 0.05f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Smooth feedback overlay - center of screen
            AnimatedVisibility(
                visible = actionFeedbackVisible,
                enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                exit = fadeOut(tween(300))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(feedbackAlpha)
                        .scale(feedbackScale)
                ) {
                    Text(
                        text = lastAction,
                        color = MaterialTheme.colors.primary,
                        style = MaterialTheme.typography.title3,
                        fontSize = 18.sp
                    )
                }
            }

            // Instructions - only when not connected, subtle
            AnimatedVisibility(
                visible = !isConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = connectionError ?: "Not Connected",
                        color = MaterialTheme.colors.error.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap to connect",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.caption2,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Connected instructions - minimal, fade in smoothly
            AnimatedVisibility(
                visible = isConnected && !actionFeedbackVisible,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(200))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(0.4f)
                ) {
                    Text(
                        text = "Tap · Hold · Double",
                        color = Color.White,
                        style = MaterialTheme.typography.caption2,
                        fontSize = 10.sp
                    )
                }
            }

            // Floating action buttons - elegant minimal design
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ESC button
                Button(
                    onClick = {
                        if (isConnected) {
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
                    currentScreen = "mouse"
                )
            }
        }
    }
}
