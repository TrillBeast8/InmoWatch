
package com.example.inmocontrol_v2.ui.screens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.inmocontrol_v2.hid.HidClient
import com.example.inmocontrol_v2.data.SettingsStore
import kotlinx.coroutines.*
@Composable fun TouchpadScreen(){
    val ctx=LocalContext.current; val store=remember{ SettingsStore.get(ctx) }; val scroll by store.scrollSensitivity.collectAsState(initial=1.0f)
    var flash by remember{ mutableStateOf(0f) }; val alpha=animateFloatAsState(targetValue=flash)
    Box(Modifier.fillMaxSize().background(Color.Black)
        .pointerInput(Unit){ detectTapGestures(onTap={ flash=1f; HidClient.instance()?.leftClick(); CoroutineScope(coroutineContext).launch{ delay(120); flash=0f } },
            onDoubleTap={ flash=1f; HidClient.instance()?.rightClick(); CoroutineScope(coroutineContext).launch{ delay(180); flash=0f } }) }
        .pointerInput(scroll){ detectTransformGestures{ c, pan, _, _ ->
            val size=this.size; val cx=size.width/2f
            val nearV=kotlin.math.abs(c.x-cx) < size.width*0.08f
            if(nearV){ val w=(pan.y*scroll).toInt(); if(w!=0) HidClient.instance()?.wheel(w) }
            else { HidClient.instance()?.move(pan.x.toInt(), pan.y.toInt()) }
        } }){
        Box(Modifier.fillMaxSize().alpha(alpha.value).background(Color.White))
        Box(Modifier.fillMaxSize()){
            Box(Modifier.fillMaxHeight().width(2.dp).background(Color(0x22FFFFFF)).align(androidx.compose.ui.Alignment.CenterHorizontally))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0x22FFFFFF)).align(androidx.compose.ui.Alignment.CenterVertically))
        }
    }
}
