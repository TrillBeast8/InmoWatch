
package com.example.inmocontrol_v2.ui.screens
import androidx.compose.foundation.clickable; import androidx.compose.foundation.layout.*; import androidx.compose.material3.Text; import androidx.compose.runtime.Composable
import androidx.compose.ui.*; import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.*; import androidx.wear.compose.foundation.lazy.*; import com.example.inmocontrol_v2.nav.Routes
@Composable fun MainMenuScreen(onNavigate:(String)->Unit){ val modes=listOf("Keyboard" to Routes.Keyboard,"Touchpad" to Routes.Touchpad,"Mouse" to Routes.Mouse,"D-Pad" to Routes.DPad,"Media" to Routes.Media,"Settings" to Routes.Settings)
    Box(Modifier.fillMaxSize()){ ScalingLazyColumn(modifier=Modifier.fillMaxSize(), horizontalAlignment=Alignment.CenterHorizontally){ item{ Text("InmoControl",fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(8.dp)) }
        items(modes){(l,r)-> Box(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{ onNavigate(r) }, contentAlignment=Alignment.Center){ Text(l) } } } } }
