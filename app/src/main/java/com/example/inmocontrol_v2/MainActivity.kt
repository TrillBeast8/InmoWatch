
package com.example.inmocontrol_v2
import android.*; import android.content.pm.PackageManager; import android.content.Intent; import android.os.*
import androidx.activity.ComponentActivity; import androidx.activity.compose.setContent; import androidx.core.app.ActivityCompat; import androidx.core.content.ContextCompat
import androidx.navigation.compose.*; import com.example.inmocontrol_v2.nav.Routes; import com.example.inmocontrol_v2.ui.screens.*; import com.example.inmocontrol_v2.ui.theme.InmoTheme; import com.example.inmocontrol_v2.hid.HidClient
class MainActivity: ComponentActivity(){ override fun onCreate(s: Bundle?){ super.onCreate(s); ensureBtPerms(); HidClient.init(application); startForegroundService(Intent(this, com.example.inmocontrol_v2.hid.HidService::class.java))
    val start=intent?.data?.getQueryParameter("route")?.ifEmpty{ null } ?: Routes.MainMenu
    setContent{ InmoTheme{ val nav=rememberNavController(); androidx.compose.runtime.LaunchedEffect(start){ if(start!=Routes.MainMenu) nav.navigate(start) }
        NavHost(nav, startDestination=Routes.MainMenu){ composable(Routes.MainMenu){ MainMenuScreen{ nav.navigate(it) } }; composable(Routes.Keyboard){ KeyboardScreen() }; composable(Routes.Touchpad){ TouchpadScreen() }; composable(Routes.Mouse){ MouseScreen() }; composable(Routes.DPad){ DpadScreen() }; composable(Routes.Media){ MediaScreen() }; composable(Routes.Settings){ SettingsScreen() } } } } }
    private fun ensureBtPerms(){ if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){ val need=arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_ADVERTISE, android.Manifest.permission.POST_NOTIFICATIONS).filter{ ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED }; if(need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 42) } } }
