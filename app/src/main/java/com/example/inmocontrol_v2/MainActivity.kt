package com.example.inmocontrol_v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inmocontrol_v2.ui.screens.*
import com.example.inmocontrol_v2.ui.theme.InmoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InmoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(onNavigate = { route: String ->
                navController.navigate(route)
            })
        }
        composable("mouse") { MouseScreen() }
        composable("keyboard") { KeyboardScreen() }
        composable("touchpad") { TouchpadScreen() }
        composable("media") { MediaScreen() }
        composable("dpad") { DpadScreen() }
        composable("settings") { SettingsScreen() }
    }
}