
package com.example.inmocontrol_v2.ui.screens
import androidx.compose.animation.core.*; import androidx.compose.foundation.gestures.onRotaryScrollEvent; import androidx.compose.foundation.layout.*; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.draw.translate; import androidx.compose.ui.input.rotary.RotaryScrollEvent; import androidx.compose.ui.unit.*
import com.example.inmocontrol_v2.hid.HidClient
@Composable fun MediaScreen(){ val title="Now Playing: Track Title That Is Rather Long For The Watch Screen"; val artist="Artist · Album"
    val t= rememberInfiniteTransition(label="marquee"); val x by t.animateFloat(0f, -400f, infiniteRepeatable(tween(8000, easing= LinearEasing), RepeatMode.Restart), label="x")
    Column(Modifier.fillMaxSize().onRotaryScrollEvent{ e:RotaryScrollEvent -> if(e.verticalScrollPixels>0) HidClient.instance()?.consumer("vol+") else HidClient.instance()?.consumer("vol-"); true }.padding(8.dp),
        horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.SpaceEvenly){
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center){ Text(title, fontSize=14.sp, modifier=Modifier.translate(x=x,y=0f)) }
        Text(artist, fontSize=10.sp)
        Row(horizontalArrangement=Arrangement.SpaceEvenly, modifier=Modifier.fillMaxWidth()){ Button({ HidClient.instance()?.consumer("rewind") }){ Text("⟲") }; Button({ HidClient.instance()?.consumer("playpause") }){ Text("⏯") }; Button({ HidClient.instance()?.consumer("forward") }){ Text("⟳") } }
        Row(horizontalArrangement=Arrangement.SpaceEvenly, modifier=Modifier.fillMaxWidth()){ Button({ HidClient.instance()?.consumer("vol-") }){ Text("Vol −") }; Button({ HidClient.instance()?.consumer("mute") }){ Text("🔈") }; Button({ HidClient.instance()?.consumer("vol+") }){ Text("Vol +") } }
    } }
