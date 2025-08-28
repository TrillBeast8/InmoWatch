
package com.example.inmocontrol_v2.ui.screens
import android.hardware.*; import androidx.compose.foundation.gestures.detectTapGestures; import androidx.compose.foundation.layout.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.input.pointer.pointerInput; import androidx.compose.ui.platform.LocalContext
import com.example.inmocontrol_v2.hid.HidClient; import com.example.inmocontrol_v2.data.SettingsStore
@Composable fun MouseScreen(){
    val ctx=LocalContext.current; val sm=ctx.getSystemService(SensorManager::class.java); val gyro=remember{ sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    val store=remember{ SettingsStore.get(ctx) }
    val scale by store.gyroSensitivity.collectAsState(initial=8f)
    val bx by store.gyroBiasX.collectAsState(initial=0f)
    val by by store.gyroBiasY.collectAsState(initial=0f)
    DisposableEffect(scale,bx,by){ val l=object: SensorEventListener{ override fun onSensorChanged(e: SensorEvent){ HidClient.instance()?.move(((e.values[1]-bx)*scale).toInt(), ((e.values[0]-by)*scale).toInt()) } override fun onAccuracyChanged(s: Sensor?, a:Int){} }
        sm.registerListener(l, gyro, SensorManager.SENSOR_DELAY_GAME); onDispose{ sm.unregisterListener(l) } }
    Box(Modifier.fillMaxSize().pointerInput(Unit){ detectTapGestures(onTap={ HidClient.instance()?.leftClick() }, onDoubleTap={ HidClient.instance()?.rightClick() }) }){} }
