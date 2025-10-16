package com.example.inmocontrol_v2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.ui.components.QuickLauncher
import kotlinx.coroutines.delay

/**
 * Elegant button-based D-pad with seamless, reliable UI for Wear OS
 * No gesture detection - just clean, responsive buttons arranged in circular pattern
 */
@Composable
fun DpadScreen(
    onBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    onScrollPopup: () -> Unit = {}
) {
    var isScrollMode by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }
    var showQuickLauncher by remember { mutableStateOf(false) }

    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Smooth feedback animations
    val feedbackAlpha by animateFloatAsState(
        targetValue = if (showFeedback) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val feedbackScale by animateFloatAsState(
        targetValue = if (showFeedback) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    // Auto-clear feedback
    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            delay(400)
            showFeedback = false
        }
    }

    // Action handler
    val handleAction = remember(isConnected, isScrollMode) {
        { direction: String, action: () -> Unit ->
            if (isConnected) {
                action()
                lastAction = direction
                showFeedback = true
            }
        }
    }

    // Disable predictive back gesture to prevent accidental navigation during D-pad usage
    androidx.activity.compose.PredictiveBackHandler(enabled = false) { }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Main D-pad layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mode indicator
                Text(
                    text = if (isScrollMode) "Scroll Mode" else "D-Pad Mode",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.primary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Top row: UP-LEFT, UP, UP-RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // UP-LEFT (diagonal)
                    if (!isScrollMode) {
                        DpadButton(
                            text = "↖",
                            onClick = { handleAction("↖") { HidClient.dpad(7) } },
                            enabled = isConnected && !isScrollMode,
                            size = 32.dp
                        )
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }

                    // UP
                    DpadButton(
                        text = "↑",
                        onClick = {
                            handleAction(if (isScrollMode) "Scroll ↑" else "↑") {
                                if (isScrollMode) HidClient.sendMouseScroll(0f, -3f)
                                else HidClient.dpad(0)
                            }
                        },
                        enabled = isConnected,
                        size = 40.dp
                    )

                    // UP-RIGHT (diagonal)
                    if (!isScrollMode) {
                        DpadButton(
                            text = "↗",
                            onClick = { handleAction("↗") { HidClient.dpad(8) } },
                            enabled = isConnected && !isScrollMode,
                            size = 32.dp
                        )
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Middle row: LEFT, CENTER, RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT
                    DpadButton(
                        text = "←",
                        onClick = {
                            handleAction(if (isScrollMode) "Scroll ←" else "←") {
                                if (isScrollMode) HidClient.sendMouseScroll(-3f, 0f)
                                else HidClient.dpad(2)
                            }
                        },
                        enabled = isConnected,
                        size = 40.dp
                    )

                    // CENTER (OK/Select or Click)
                    DpadButton(
                        text = "●",
                        onClick = {
                            handleAction(if (isScrollMode) "Click" else "OK") {
                                if (isScrollMode) HidClient.sendLeftClick()
                                else HidClient.sendDpadCenter()
                            }
                        },
                        enabled = isConnected,
                        size = 44.dp,
                        primary = true
                    )

                    // RIGHT
                    DpadButton(
                        text = "→",
                        onClick = {
                            handleAction(if (isScrollMode) "Scroll →" else "→") {
                                if (isScrollMode) HidClient.sendMouseScroll(3f, 0f)
                                else HidClient.dpad(3)
                            }
                        },
                        enabled = isConnected,
                        size = 40.dp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom row: DOWN-LEFT, DOWN, DOWN-RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DOWN-LEFT (diagonal)
                    if (!isScrollMode) {
                        DpadButton(
                            text = "↙",
                            onClick = { handleAction("↙") { HidClient.dpad(5) } },
                            enabled = isConnected && !isScrollMode,
                            size = 32.dp
                        )
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }

                    // DOWN
                    DpadButton(
                        text = "↓",
                        onClick = {
                            handleAction(if (isScrollMode) "Scroll ↓" else "↓") {
                                if (isScrollMode) HidClient.sendMouseScroll(0f, 3f)
                                else HidClient.dpad(1)
                            }
                        },
                        enabled = isConnected,
                        size = 40.dp
                    )

                    // DOWN-RIGHT (diagonal)
                    if (!isScrollMode) {
                        DpadButton(
                            text = "↘",
                            onClick = { handleAction("↘") { HidClient.dpad(6) } },
                            enabled = isConnected && !isScrollMode,
                            size = 32.dp
                        )
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode toggle and ESC button row (compact icon-only design)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode toggle button (icon only)
                    Button(
                        onClick = { isScrollMode = !isScrollMode },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isScrollMode)
                                MaterialTheme.colors.secondary.copy(alpha = 0.4f)
                            else MaterialTheme.colors.primary.copy(alpha = 0.4f)
                        ),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isScrollMode) Icons.Default.SwapVert else Icons.Default.Apps,
                            contentDescription = if (isScrollMode) "Scroll Mode" else "D-Pad Mode",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ESC button
                    Button(
                        onClick = {
                            handleAction("ESC") {
                                HidClient.sendEscape()
                            }
                        },
                        enabled = isConnected,
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.4f)
                        ),
                        shape = CircleShape
                    ) {
                        Text("⎋", fontSize = 16.sp)
                    }
                }
            }

            // Action feedback overlay
            AnimatedVisibility(
                visible = showFeedback,
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
                        text = lastAction,
                        style = MaterialTheme.typography.display1,
                        color = MaterialTheme.colors.secondary,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Connection error
            AnimatedVisibility(
                visible = !isConnected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                ) {
                    Text(
                        text = connectionError ?: "Not Connected",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption1,
                        fontSize = 10.sp
                    )
                }
            }

            // Quick Launcher button
            Button(
                onClick = { showQuickLauncher = !showQuickLauncher },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .size(36.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (showQuickLauncher) MaterialTheme.colors.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colors.surface.copy(alpha = 0.4f)
                )
            ) {
                Text("⋮", fontSize = 16.sp)
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
                    currentScreen = "dpad"
                )
            }
        }
    }
}

/**
 * Clean D-pad button component
 */
@Composable
fun DpadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp,
    primary: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = when {
                primary -> MaterialTheme.colors.primary.copy(alpha = 0.5f)
                enabled -> MaterialTheme.colors.surface.copy(alpha = 0.4f)
                else -> MaterialTheme.colors.surface.copy(alpha = 0.15f)
            },
            disabledBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.1f)
        ),
        shape = CircleShape
    ) {
        Text(
            text = text,
            fontSize = (size.value * 0.5f).sp,
            color = if (enabled) Color.White else Color.Gray.copy(alpha = 0.3f)
        )
    }
}
