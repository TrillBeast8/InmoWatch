package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.data.SettingsStore
import com.example.inmocontrol_v2.hid.HidClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * KeyboardScreen - System keyboard with HID transmission
 */
@Composable
fun KeyboardScreen(
    onBack: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()

    // Simple string-based state management
    var textBuffer by remember { mutableStateOf("") }
    var lastSentText by remember { mutableStateOf("") }
    var showSentFeedback by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()
    val isRealtimeMode by settingsStore.realtimeKeyboard.collectAsState(initial = false)

    // Auto-clear feedback
    LaunchedEffect(showSentFeedback) {
        if (showSentFeedback) {
            delay(2000)
            showSentFeedback = false
        }
    }

    // Character to HID scan code mapping
    val charToKeyMap = remember {
        mapOf(
            ' ' to (0x2C to 0), '\n' to (0x28 to 0), '\t' to (0x2B to 0),
            'a' to (0x04 to 0), 'b' to (0x05 to 0), 'c' to (0x06 to 0), 'd' to (0x07 to 0),
            'e' to (0x08 to 0), 'f' to (0x09 to 0), 'g' to (0x0A to 0), 'h' to (0x0B to 0),
            'i' to (0x0C to 0), 'j' to (0x0D to 0), 'k' to (0x0E to 0), 'l' to (0x0F to 0),
            'm' to (0x10 to 0), 'n' to (0x11 to 0), 'o' to (0x12 to 0), 'p' to (0x13 to 0),
            'q' to (0x14 to 0), 'r' to (0x15 to 0), 's' to (0x16 to 0), 't' to (0x17 to 0),
            'u' to (0x18 to 0), 'v' to (0x19 to 0), 'w' to (0x1A to 0), 'x' to (0x1B to 0),
            'y' to (0x1C to 0), 'z' to (0x1D to 0),
            'A' to (0x04 to 0x02), 'B' to (0x05 to 0x02), 'C' to (0x06 to 0x02), 'D' to (0x07 to 0x02),
            'E' to (0x08 to 0x02), 'F' to (0x09 to 0x02), 'G' to (0x0A to 0x02), 'H' to (0x0B to 0x02),
            'I' to (0x0C to 0x02), 'J' to (0x0D to 0x02), 'K' to (0x0E to 0x02), 'L' to (0x0F to 0x02),
            'M' to (0x10 to 0x02), 'N' to (0x11 to 0x02), 'O' to (0x12 to 0x02), 'P' to (0x13 to 0x02),
            'Q' to (0x14 to 0x02), 'R' to (0x15 to 0x02), 'S' to (0x16 to 0x02), 'T' to (0x17 to 0x02),
            'U' to (0x18 to 0x02), 'V' to (0x19 to 0x02), 'W' to (0x1A to 0x02), 'X' to (0x1B to 0x02),
            'Y' to (0x1C to 0x02), 'Z' to (0x1D to 0x02),
            '1' to (0x1E to 0), '2' to (0x1F to 0), '3' to (0x20 to 0), '4' to (0x21 to 0),
            '5' to (0x22 to 0), '6' to (0x23 to 0), '7' to (0x24 to 0), '8' to (0x25 to 0),
            '9' to (0x26 to 0), '0' to (0x27 to 0),
            '.' to (0x37 to 0), ',' to (0x36 to 0), '?' to (0x38 to 0x02), '!' to (0x1E to 0x02),
            ';' to (0x33 to 0), ':' to (0x33 to 0x02)
        )
    }

    // Send character as HID key
    val sendCharacterAsKey: (Char) -> Unit = remember {
        { char ->
            val (keyCode, modifiers) = charToKeyMap[char] ?: (0x2C to 0)
            HidClient.sendKey(keyCode, modifiers)
        }
    }

    // Handle text changes for realtime mode
    LaunchedEffect(textBuffer, isRealtimeMode, isConnected) {
        if (isRealtimeMode && isConnected && textBuffer != lastSentText) {
            if (textBuffer.length < lastSentText.length) {
                // Handle backspace
                repeat(lastSentText.length - textBuffer.length) {
                    HidClient.sendKey(0x2A)
                    delay(5)
                }
            } else {
                // Send new characters
                val newChars = textBuffer.substring(lastSentText.length)
                newChars.forEach { char ->
                    sendCharacterAsKey(char)
                    delay(5)
                }
            }
            lastSentText = textBuffer
        }
    }

    // Send text function for deferred mode
    val sendText: (String) -> Unit = remember {
        { text ->
            if (isConnected && text.isNotEmpty()) {
                text.forEach { char ->
                    sendCharacterAsKey(char)
                    Thread.sleep(5)
                }
                if (!isRealtimeMode) {
                    lastSentText = text
                    showSentFeedback = true
                    textBuffer = ""
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
            // Title
            item {
                Text(
                    text = if (isRealtimeMode) "Keyboard-RT" else "Keyboard-D",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary
                )
            }

            // Connection status
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

            // Text input
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

                    BasicTextField(
                        value = textBuffer,
                        onValueChange = { textBuffer = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (textBuffer.isNotEmpty()) {
                                    sendText(textBuffer)
                                }
                            }
                        )
                    )
                }
            }

            // Feedback for deferred mode
            if (showSentFeedback && !isRealtimeMode) {
                item {
                    Text(
                        text = "✓ Sent: \"$lastSentText\"",
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Control buttons
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Send button (deferred mode only)
                    if (!isRealtimeMode) {
                        Button(
                            onClick = { sendText(textBuffer) },
                            enabled = isConnected && textBuffer.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Send", fontSize = 10.sp)
                        }
                    }

                    // Clear button
                    Button(
                        onClick = {
                            textBuffer = ""
                            lastSentText = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear", fontSize = 10.sp)
                    }

                    // Back button
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", fontSize = 10.sp)
                    }
                }
            }

            // Mode toggle
            item {
                ToggleChip(
                    checked = isRealtimeMode,
                    onCheckedChange = {
                        scope.launch {
                            settingsStore.setRealtimeKeyboard(it)
                        }
                    },
                    label = { Text("Realtime Mode") },
                    toggleControl = {
                        Switch(
                            checked = isRealtimeMode,
                            onCheckedChange = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
