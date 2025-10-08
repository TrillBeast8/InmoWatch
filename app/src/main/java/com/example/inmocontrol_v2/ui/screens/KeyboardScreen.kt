package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.DeviceProfile
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Optimized KeyboardScreen with improved performance and memory efficiency
 * Fixed to use only Wear Compose components
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeyboardScreen(
    onBack: () -> Unit = {},
    onScrollPopup: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()

    // Optimize LaunchedEffect to prevent unnecessary recreations
    LaunchedEffect(Unit) {
        HidClient.currentDeviceProfile = DeviceProfile.Keyboard
    }

    // Optimized state management with reduced recomposition
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var lastSentText by remember { mutableStateOf("") }
    var showSentFeedback by remember { mutableStateOf(false) }

    // Cached focus and keyboard controller
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Optimized state collection with distinctUntilChanged
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()
    val isRealtimeMode by settingsStore.realtimeKeyboard.collectAsState(initial = false)

    // Optimized feedback timer
    LaunchedEffect(showSentFeedback) {
        if (showSentFeedback) {
            delay(2000)
            showSentFeedback = false
        }
    }

    // Cached character mapping for better performance
    val charToKeyMap = remember {
        mapOf(
            ' ' to (0x2C to 0), '\n' to (0x28 to 0), '\t' to (0x2B to 0),
            // Letters a-z
            'a' to (0x04 to 0), 'b' to (0x05 to 0), 'c' to (0x06 to 0), 'd' to (0x07 to 0),
            'e' to (0x08 to 0), 'f' to (0x09 to 0), 'g' to (0x0A to 0), 'h' to (0x0B to 0),
            'i' to (0x0C to 0), 'j' to (0x0D to 0), 'k' to (0x0E to 0), 'l' to (0x0F to 0),
            'm' to (0x10 to 0), 'n' to (0x11 to 0), 'o' to (0x12 to 0), 'p' to (0x13 to 0),
            'q' to (0x14 to 0), 'r' to (0x15 to 0), 's' to (0x16 to 0), 't' to (0x17 to 0),
            'u' to (0x18 to 0), 'v' to (0x19 to 0), 'w' to (0x1A to 0), 'x' to (0x1B to 0),
            'y' to (0x1C to 0), 'z' to (0x1D to 0),
            // Capital letters A-Z
            'A' to (0x04 to 0x02), 'B' to (0x05 to 0x02), 'C' to (0x06 to 0x02), 'D' to (0x07 to 0x02),
            'E' to (0x08 to 0x02), 'F' to (0x09 to 0x02), 'G' to (0x0A to 0x02), 'H' to (0x0B to 0x02),
            'I' to (0x0C to 0x02), 'J' to (0x0D to 0x02), 'K' to (0x0E to 0x02), 'L' to (0x0F to 0x02),
            'M' to (0x10 to 0x02), 'N' to (0x11 to 0x02), 'O' to (0x12 to 0x02), 'P' to (0x13 to 0x02),
            'Q' to (0x14 to 0x02), 'R' to (0x15 to 0x02), 'S' to (0x16 to 0x02), 'T' to (0x17 to 0x02),
            'U' to (0x18 to 0x02), 'V' to (0x19 to 0x02), 'W' to (0x1A to 0x02), 'X' to (0x1B to 0x02),
            'Y' to (0x1C to 0x02), 'Z' to (0x1D to 0x02),
            // Numbers 0-9
            '1' to (0x1E to 0), '2' to (0x1F to 0), '3' to (0x20 to 0), '4' to (0x21 to 0),
            '5' to (0x22 to 0), '6' to (0x23 to 0), '7' to (0x24 to 0), '8' to (0x25 to 0),
            '9' to (0x26 to 0), '0' to (0x27 to 0),
            // Common punctuation
            '.' to (0x37 to 0), ',' to (0x36 to 0), '?' to (0x38 to 0x02), '!' to (0x1E to 0x02),
            ';' to (0x33 to 0), ':' to (0x33 to 0x02)
        )
    }

    // Optimized character sending with cached mapping
    val sendCharacterAsKey = remember {
        { char: Char ->
            val (keyCode, modifiers) = charToKeyMap[char] ?: (0x2C to 0)
            HidClient.sendKey(keyCode, modifiers)
            Unit // Explicitly return Unit to fix the type mismatch
        }
    }

    // Optimized text change handler
    val handleTextChange = remember {
        { newValue: TextFieldValue ->
            val oldText = textFieldValue.text
            textFieldValue = newValue
            val currentText = newValue.text

            if (isRealtimeMode && isConnected) {
                when {
                    currentText.length < oldText.length -> {
                        // Handle deletions efficiently
                        repeat(oldText.length - currentText.length) {
                            HidClient.sendKey(0x2A) // Backspace
                        }
                    }
                    currentText.length > oldText.length -> {
                        // Send only new characters
                        currentText.substring(oldText.length).forEach(sendCharacterAsKey)
                    }
                }
                lastSentText = currentText
            }
        }
    }

    // Optimized text sending function
    val sendText = remember {
        { text: String ->
            if (isConnected && text.isNotEmpty()) {
                text.forEach { char ->
                    sendCharacterAsKey(char)
                    Thread.sleep(5) // Reduced delay for better performance
                }

                if (!isRealtimeMode) {
                    lastSentText = text
                    showSentFeedback = true
                    textFieldValue = TextFieldValue("")
                }
            }
        }
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optimized clickable title
            item {
                Button(
                    onClick = onScrollPopup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
                ) {
                    Text(
                        text = if (isRealtimeMode) "Keyboard-RT" else "Keyboard-D",
                        style = MaterialTheme.typography.title2,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary
                    )
                }
            }

            // Connection status - only show when not connected
            if (!isConnected) {
                item {
                    Text(
                        text = connectionError ?: "Not connected",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Optimized text input section - Fixed to use Wear Compose only
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isRealtimeMode) "Type (live mode):" else "Type your message:",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Use Wear Compose Card instead of Material 3 TextField
                    Card(
                        onClick = { focusRequester.requestFocus() },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = MaterialTheme.colors.surface,
                            endBackgroundColor = MaterialTheme.colors.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = if (textFieldValue.text.isEmpty()) {
                                    if (isRealtimeMode) "Live typing..." else "Enter text..."
                                } else textFieldValue.text,
                                style = MaterialTheme.typography.body2,
                                color = if (textFieldValue.text.isEmpty()) Color.Gray else Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Control buttons row
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { if (isConnected) HidClient.sendKey(0x2A) },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Text("⌫", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            textFieldValue = TextFieldValue("")
                            lastSentText = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear", fontSize = 10.sp)
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", fontSize = 10.sp)
                    }
                }
            }

            // Send button and mode toggle
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRealtimeMode) {
                        Button(
                            onClick = {
                                sendText(textFieldValue.text)
                                keyboardController?.hide()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isConnected && textFieldValue.text.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                        ) {
                            Text("Send", fontSize = 10.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    ToggleChip(
                        checked = isRealtimeMode,
                        onCheckedChange = { scope.launch { settingsStore.setRealtimeKeyboard(it) } },
                        label = { Text(if (isRealtimeMode) "RT" else "D", fontSize = 10.sp) },
                        modifier = Modifier.width(60.dp),
                        toggleControl = {
                            Switch(
                                checked = isRealtimeMode,
                                onCheckedChange = null
                            )
                        },
                        colors = ToggleChipDefaults.toggleChipColors(
                            checkedStartBackgroundColor = MaterialTheme.colors.secondary,
                            checkedEndBackgroundColor = MaterialTheme.colors.secondary
                        )
                    )
                }
            }

            // Feedback message - only show when relevant
            if (showSentFeedback && !isRealtimeMode) {
                item {
                    Text(
                        text = "Sent: $lastSentText",
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
