package com.example.inmocontrol_v2.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme as WearMaterialTheme

/**
 * InmoTheme - Proper Wear OS theme that preserves your original UI design
 * Fixed to work with Wear Compose Material components
 */

// Wear OS color scheme that matches your original design
private val WearColorScheme = Colors(
    primary = Color(0xFF6650a4),
    primaryVariant = Color(0xFF3700B3),
    secondary = Color(0xFF625b71),
    secondaryVariant = Color(0xFF018786),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

@Composable
fun InmoTheme(
    content: @Composable () -> Unit
) {
    // Use Wear Material Theme for all your screens
    WearMaterialTheme(
        colors = WearColorScheme,
        content = content
    )
}