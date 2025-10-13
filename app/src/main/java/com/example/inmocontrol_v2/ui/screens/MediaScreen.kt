package com.example.inmocontrol_v2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.ui.components.QuickLauncher
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.abs

/**
 * Elegant MediaScreen with seamless circular gesture controls + Bezel/Rotary volume control
 * Matches the fluid, swift design of MouseScreen and TouchpadScreen
 */
@Composable
fun MediaScreen(
    onBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    // Elegant state management
    var isPlaying by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }
    var actionFeedbackVisible by remember { mutableStateOf(false) }
    var showDpad by remember { mutableStateOf(false) }
    var showQuickLauncher by remember { mutableStateOf(false) }

    // Connection state
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Smooth feedback animations with spring physics
    val feedbackAlpha by animateFloatAsState(
        targetValue = if (actionFeedbackVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val feedbackScale by animateFloatAsState(
        targetValue = if (actionFeedbackVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    // Auto-clear feedback - quick and responsive
    LaunchedEffect(actionFeedbackVisible) {
        if (actionFeedbackVisible) {
            delay(500)
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

    // Handle circular gesture detection for media controls
    fun handleCircularGesture(offset: Offset, centerOffset: Offset) {
        if (!isConnected) return

        val deltaX = offset.x - centerOffset.x
        val deltaY = offset.y - centerOffset.y
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

        // Center zone = Play/Pause
        if (distance < 60f) {
            isPlaying = !isPlaying
            performAction(if (isPlaying) "▶ Play" else "⏸ Pause") { HidClient.playPause() }
            return
        }

        // Calculate angle for swipe gestures
        val angle = Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()

        // Detect swipe direction (simplified to left/right for prev/next)
        when {
            angle >= -45 && angle < 45 -> {
                // Right swipe = Next Track
                performAction("Next ⏭") { HidClient.nextTrack() }
            }
            angle >= 135 || angle < -135 -> {
                // Left swipe = Previous Track
                performAction("⏮ Prev") { HidClient.previousTrack() }
            }
            angle >= 45 && angle < 135 -> {
                // Down swipe = Volume Down
                performAction("Vol -") { HidClient.volumeDown() }
            }
            angle >= -135 && angle < -45 -> {
                // Up swipe = Volume Up
                performAction("Vol +") { HidClient.volumeUp() }
            }
        }
    }

    // Cache colors outside Canvas for performance and to avoid @Composable invocation errors
    val primaryColor = MaterialTheme.colors.primary
    val errorColor = MaterialTheme.colors.error
    val secondaryColor = MaterialTheme.colors.secondary
    val surfaceColor = MaterialTheme.colors.surface

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier
            .onRotaryScrollEvent { event ->
                // Bezel/Rotary input for volume control
                if (isConnected) {
                    val delta = event.verticalScrollPixels
                    if (delta > 0) {
                        HidClient.volumeUp()
                        performAction("Vol +") { }
                    } else if (delta < 0) {
                        HidClient.volumeDown()
                        performAction("Vol -") { }
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Full-screen circular touch area for seamless gesture control
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            handleCircularGesture(offset, center)
                        }
                    }
                    .pointerInput(Unit) {
                        var dragStartOffset = Offset.Zero
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartOffset = offset
                            },
                            onDragEnd = {
                                // Process swipe on drag end
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val deltaX = dragStartOffset.x - center.x
                                val deltaY = dragStartOffset.y - center.y
                                
                                // Detect significant horizontal swipe
                                if (abs(deltaX) > abs(deltaY) && abs(deltaX) > 50f) {
                                    if (deltaX > 0) {
                                        // Swipe right = Next
                                        performAction("Next ⏭") { HidClient.nextTrack() }
                                    } else {
                                        // Swipe left = Previous
                                        performAction("⏮ Prev") { HidClient.previousTrack() }
                                    }
                                }
                                // Detect significant vertical swipe
                                else if (abs(deltaY) > abs(deltaX) && abs(deltaY) > 50f) {
                                    if (deltaY < 0) {
                                        // Swipe up = Volume Up
                                        performAction("Vol +") { HidClient.volumeUp() }
                                    } else {
                                        // Swipe down = Volume Down
                                        performAction("Vol -") { HidClient.volumeDown() }
                                    }
                                }
                            }
                        ) { _, _ -> }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Elegant circular guide with subtle presence
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension * 0.42f
                    
                    // Subtle outer ring
                    drawCircle(
                        color = if (isConnected) Color.White.copy(alpha = 0.15f)
                               else Color.Gray.copy(alpha = 0.1f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Inner center circle for play/pause zone
                    drawCircle(
                        color = if (isConnected) primaryColor.copy(alpha = 0.2f)
                               else Color.Gray.copy(alpha = 0.08f),
                        radius = 60f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    
                    // Directional guides (subtle)
                    val guideRadius = radius * 0.8f
                    // Up arrow guide
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = center.copy(y = center.y - guideRadius),
                        end = center.copy(y = center.y - guideRadius + 15f),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Down arrow guide
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = center.copy(y = center.y + guideRadius),
                        end = center.copy(y = center.y + guideRadius - 15f),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Left arrow guide
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = center.copy(x = center.x - guideRadius),
                        end = center.copy(x = center.x - guideRadius + 15f),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Right arrow guide
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = center.copy(x = center.x + guideRadius),
                        end = center.copy(x = center.x + guideRadius - 15f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Center play/pause indicator
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isConnected) primaryColor.copy(alpha = 0.6f)
                               else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Directional labels (elegant arrows)
                DirectionalMediaLabel("↑", 0f, -100f, "Vol +")
                DirectionalMediaLabel("↓", 0f, 100f, "Vol -")
                DirectionalMediaLabel("←", -100f, 0f, "Prev")
                DirectionalMediaLabel("→", 100f, 0f, "Next")
                
                // Action feedback overlay with smooth animations
                AnimatedVisibility(
                    visible = actionFeedbackVisible,
                    enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                    exit = fadeOut(tween(400))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(feedbackAlpha)
                            .scale(feedbackScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = lastAction,
                                style = MaterialTheme.typography.display1,
                                color = MaterialTheme.colors.secondary,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Connection status
                AnimatedVisibility(
                    visible = !isConnected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.offset(y = (-60).dp)
                    ) {
                        Text(
                            text = connectionError ?: "Not Connected",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption1,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Floating action buttons - bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // D-Pad quick launch
                Button(
                    onClick = { showDpad = !showDpad },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (showDpad) MaterialTheme.colors.primary.copy(alpha = 0.6f)
                                        else MaterialTheme.colors.surface.copy(alpha = 0.4f)
                    )
                ) {
                    Text("⊞", fontSize = 16.sp)
                }
                
                // Quick Launcher button (replaces X/Back)
                Button(
                    onClick = { showQuickLauncher = !showQuickLauncher },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (showQuickLauncher) MaterialTheme.colors.primary.copy(alpha = 0.6f)
                                        else MaterialTheme.colors.surface.copy(alpha = 0.4f)
                    )
                ) {
                    Text("⋮", fontSize = 16.sp)
                }
            }
            
            // Quick Launcher overlay (when toggled)
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
                    currentScreen = "media"
                )
            }
        }
    }
}

/**
 * Directional media label with hint text
 */
@Composable
fun BoxScope.DirectionalMediaLabel(arrow: String, offsetX: Float, offsetY: Float, hint: String) {
    Box(
        modifier = Modifier
            .offset(offsetX.dp, offsetY.dp)
            .size(36.dp)
            .align(Alignment.Center),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = arrow,
                style = MaterialTheme.typography.caption2,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 20.sp
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.caption2,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 8.sp
            )
        }
    }
}
