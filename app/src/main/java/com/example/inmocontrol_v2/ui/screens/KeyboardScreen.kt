package com.example.inmocontrol_v2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.DeviceProfile
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeyboardScreen(
    onBack: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        HidClient.currentDeviceProfile = DeviceProfile.Keyboard
    }

    var textInput by remember { mutableStateOf("") }
    var lastSentMessage by remember { mutableStateOf("") }
    var showSentFeedback by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Use real-time connection state from HidClient
    val isConnected by HidClient.isConnected.collectAsState()
    val connectionError by HidClient.connectionError.collectAsState()

    // Auto-hide sent feedback after 2 seconds
    LaunchedEffect(showSentFeedback) {
        if (showSentFeedback) {
            delay(2000)
            showSentFeedback = false
        }
    }

    fun sendText(text: String) {
        if (isConnected && text.isNotEmpty()) {
            // Send each character as a key press with proper HID key codes
            text.forEach { char ->
                val (keyCode, modifiers) = when (char) {
                    ' ' -> Pair(0x2C, 0) // SPACE
                    '\n' -> Pair(0x28, 0) // ENTER
                    '\t' -> Pair(0x2B, 0) // TAB

                    // Letters (a-z)
                    'a' -> Pair(0x04, 0)
                    'b' -> Pair(0x05, 0)
                    'c' -> Pair(0x06, 0)
                    'd' -> Pair(0x07, 0)
                    'e' -> Pair(0x08, 0)
                    'f' -> Pair(0x09, 0)
                    'g' -> Pair(0x0A, 0)
                    'h' -> Pair(0x0B, 0)
                    'i' -> Pair(0x0C, 0)
                    'j' -> Pair(0x0D, 0)
                    'k' -> Pair(0x0E, 0)
                    'l' -> Pair(0x0F, 0)
                    'm' -> Pair(0x10, 0)
                    'n' -> Pair(0x11, 0)
                    'o' -> Pair(0x12, 0)
                    'p' -> Pair(0x13, 0)
                    'q' -> Pair(0x14, 0)
                    'r' -> Pair(0x15, 0)
                    's' -> Pair(0x16, 0)
                    't' -> Pair(0x17, 0)
                    'u' -> Pair(0x18, 0)
                    'v' -> Pair(0x19, 0)
                    'w' -> Pair(0x1A, 0)
                    'x' -> Pair(0x1B, 0)
                    'y' -> Pair(0x1C, 0)
                    'z' -> Pair(0x1D, 0)

                    // Capital letters (with shift)
                    'A' -> Pair(0x04, 0x02)
                    'B' -> Pair(0x05, 0x02)
                    'C' -> Pair(0x06, 0x02)
                    'D' -> Pair(0x07, 0x02)
                    'E' -> Pair(0x08, 0x02)
                    'F' -> Pair(0x09, 0x02)
                    'G' -> Pair(0x0A, 0x02)
                    'H' -> Pair(0x0B, 0x02)
                    'I' -> Pair(0x0C, 0x02)
                    'J' -> Pair(0x0D, 0x02)
                    'K' -> Pair(0x0E, 0x02)
                    'L' -> Pair(0x0F, 0x02)
                    'M' -> Pair(0x10, 0x02)
                    'N' -> Pair(0x11, 0x02)
                    'O' -> Pair(0x12, 0x02)
                    'P' -> Pair(0x13, 0x02)
                    'Q' -> Pair(0x14, 0x02)
                    'R' -> Pair(0x15, 0x02)
                    'S' -> Pair(0x16, 0x02)
                    'T' -> Pair(0x17, 0x02)
                    'U' -> Pair(0x18, 0x02)
                    'V' -> Pair(0x19, 0x02)
                    'W' -> Pair(0x1A, 0x02)
                    'X' -> Pair(0x1B, 0x02)
                    'Y' -> Pair(0x1C, 0x02)
                    'Z' -> Pair(0x1D, 0x02)

                    // Numbers
                    '1' -> Pair(0x1E, 0)
                    '2' -> Pair(0x1F, 0)
                    '3' -> Pair(0x20, 0)
                    '4' -> Pair(0x21, 0)
                    '5' -> Pair(0x22, 0)
                    '6' -> Pair(0x23, 0)
                    '7' -> Pair(0x24, 0)
                    '8' -> Pair(0x25, 0)
                    '9' -> Pair(0x26, 0)
                    '0' -> Pair(0x27, 0)

                    // Common punctuation
                    '.' -> Pair(0x37, 0)
                    ',' -> Pair(0x36, 0)
                    '?' -> Pair(0x38, 0x02) // / with shift
                    '!' -> Pair(0x1E, 0x02) // 1 with shift
                    ';' -> Pair(0x33, 0)
                    ':' -> Pair(0x33, 0x02) // ; with shift

                    else -> Pair(0x2C, 0) // Default to space for unsupported characters
                }

                HidClient.sendKey(keyCode, modifiers)
                Thread.sleep(10) // Small delay between characters
            }

            lastSentMessage = text
            showSentFeedback = true
            textInput = "" // Clear input after sending
        }
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Keyboard",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
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

            // Text input field with system keyboard
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Type your message:",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            androidx.compose.material3.Text(
                                text = "Enter text...",
                                fontSize = 12.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                sendText(textInput)
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = false,
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colors.primary,
                            unfocusedIndicatorColor = Color.Gray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray
                        )
                    )
                }
            }

            // Send button
            item {
                Button(
                    onClick = {
                        sendText(textInput)
                        keyboardController?.hide()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConnected && textInput.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Text("Send", style = MaterialTheme.typography.body2)
                }
            }

            // Quick action buttons
            if (isConnected) {
                item {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.caption1,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompactChip(
                            onClick = { HidClient.sendKey(0x28) }, // ENTER
                            label = { Text("Enter", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactChip(
                            onClick = { HidClient.sendKey(0x2C) }, // SPACE
                            label = { Text("Space", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompactChip(
                            onClick = { HidClient.sendKey(0x2A) }, // BACKSPACE
                            label = { Text("Back", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactChip(
                            onClick = { HidClient.sendKey(0x2B) }, // TAB
                            label = { Text("Tab", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Quick text shortcuts
                item {
                    Text(
                        text = "Quick Text",
                        style = MaterialTheme.typography.caption1,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompactChip(
                            onClick = { sendText("Hello") },
                            label = { Text("Hello", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactChip(
                            onClick = { sendText("Thank you") },
                            label = { Text("Thanks", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompactChip(
                            onClick = { sendText("Yes") },
                            label = { Text("Yes", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        CompactChip(
                            onClick = { sendText("No") },
                            label = { Text("No", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Feedback message
            if (showSentFeedback) {
                item {
                    Card(
                        onClick = { /* Feedback display - no action needed */ },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "Sent: \"$lastSentMessage\"",
                            style = MaterialTheme.typography.caption1,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Back button
            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.surface
                    )
                ) {
                    Text("Back", style = MaterialTheme.typography.body2)
                }
            }
        }
    }

    // Auto-focus the text field when screen opens
    LaunchedEffect(Unit) {
        delay(500) // Small delay to ensure screen is ready
        focusRequester.requestFocus()
    }
}
