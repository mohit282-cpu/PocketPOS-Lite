package com.example.pocketpos_lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.pocketpos_lite.ui.navigation.NavGraph
import com.example.pocketpos_lite.ui.theme.PocketPOSLiteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketPOSLiteTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
