
package com.example.inmocontrol_v2.ui.screens
import androidx.compose.foundation.layout.*; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.platform.LocalContext; import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.hid.HidClient; import com.example.inmocontrol_v2.data.SettingsStore
@Composable fun DpadScreen(){ val ctx=LocalContext.current; val store=remember{ SettingsStore.get(ctx) }; val eight by store.dpadEightWay.collectAsState(initial=true)
    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement=Arrangement.SpaceEvenly){
        Row(horizontalArrangement=Arrangement.SpaceEvenly, modifier=Modifier.fillMaxWidth()){ if(eight) Button({ HidClient.instance()?.dpad("upleft") }){ Text("↖") }; Button({ HidClient.instance()?.dpad("up") }){ Text("▲") }; if(eight) Button({ HidClient.instance()?.dpad("upright") }){ Text("↗") } }
        Row(horizontalArrangement=Arrangement.SpaceEvenly, modifier=Modifier.fillMaxWidth()){ Button({ HidClient.instance()?.dpad("left") }){ Text("◀") }; Button({ HidClient.instance()?.dpad("ok") }){ Text("●") }; Button({ HidClient.instance()?.dpad("right") }){ Text("▶") } }
        Row(horizontalArrangement=Arrangement.SpaceEvenly, modifier=Modifier.fillMaxWidth()){ if(eight) Button({ HidClient.instance()?.dpad("downleft") }){ Text("↙") }; Button({ HidClient.instance()?.dpad("down") }){ Text("▼") }; if(eight) Button({ HidClient.instance()?.dpad("downright") }){ Text("↘") } }
    } }
