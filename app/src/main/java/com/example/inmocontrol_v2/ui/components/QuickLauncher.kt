package com.example.inmocontrol_v2.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay

/**
 * Universal Quick Launcher - Beautiful circular menu to jump to any mode
 * Replaces the X button across all screens for seamless navigation
 */
@Composable
fun QuickLauncher(
    onClose: () -> Unit,
    onNavigateTo: (String) -> Unit,
    currentScreen: String
) {
    var selectedMode by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            delay(300)
            showFeedback = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.85f))
        }

        // Circular menu layout
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Title
            Text(
                text = "Quick Launch",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            // Mode buttons in circular layout
            val modes = listOf(
                ModeItem("Mouse", Icons.Default.Mouse, "mouse"),
                ModeItem("Touchpad", Icons.Default.TouchApp, "touchpad"),
                ModeItem("Keyboard", Icons.Default.Keyboard, "keyboard"),
                ModeItem("Media", Icons.Default.MusicNote, "media"),
                ModeItem("D-Pad", Icons.Default.Games, "dpad"),
                ModeItem("Settings", Icons.Default.Settings, "settings")
            )

            // Circular arrangement
            modes.forEachIndexed { index, mode ->
                val angle = (index * 60f - 90f) * (Math.PI / 180f)
                val radius = 70.dp.value
                val offsetX = (radius * kotlin.math.cos(angle)).dp
                val offsetY = (radius * kotlin.math.sin(angle)).dp

                Box(
                    modifier = Modifier
                        .offset(offsetX, offsetY)
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (mode.route != currentScreen) {
                                selectedMode = mode.name
                                showFeedback = true
                                onNavigateTo(mode.route)
                            }
                        }
                        .alpha(if (mode.route == currentScreen) 0.5f else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = mode.name,
                            tint = if (mode.route == currentScreen) MaterialTheme.colors.primary
                                   else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.caption2,
                            color = if (mode.route == currentScreen) MaterialTheme.colors.primary
                                    else Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Center close button
            Button(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.8f)
                )
            ) {
                Text("✕", fontSize = 20.sp)
            }

            // Feedback
            if (showFeedback) {
                Text(
                    text = "→ $selectedMode",
                    style = MaterialTheme.typography.display1,
                    color = MaterialTheme.colors.secondary,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}

private data class ModeItem(
    val name: String,
    val icon: ImageVector,
    val route: String
)

