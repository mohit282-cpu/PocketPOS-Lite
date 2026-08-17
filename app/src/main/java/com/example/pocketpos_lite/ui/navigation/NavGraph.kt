package com.example.pocketpos_lite.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pocketpos_lite.feature.startup.StartupScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Startup.route
    ) {
        composable(Screen.Startup.route) {
            StartupScreen()
        }
    }
}
