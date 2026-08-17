package com.example.pocketpos_lite.ui.navigation

sealed class Screen(val route: String) {
    object Startup : Screen("startup")
    // Future screens:
    // object Auth : Screen("auth")
    // object Dashboard : Screen("dashboard")
}
