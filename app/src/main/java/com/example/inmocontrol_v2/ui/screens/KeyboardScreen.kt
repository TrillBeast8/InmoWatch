
package com.example.inmocontrol_v2.ui.screens
import androidx.compose.foundation.layout.*; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.text.input.TextFieldValue; import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.hid.HidClient
@Composable fun KeyboardScreen(){ var v by remember{ mutableStateOf(TextFieldValue("")) }; Column(Modifier.fillMaxSize().padding(8.dp)){ TextField(Modifier.fillMaxWidth().weight(1f), v, { v=it }, placeholder={ Text("Type – system keyboard will appear") }, maxLines=6); Spacer(Modifier.height(6.dp)); Button(onClick={ HidClient.instance()?.sendText(v.text) }){ Text("Send") } } }
