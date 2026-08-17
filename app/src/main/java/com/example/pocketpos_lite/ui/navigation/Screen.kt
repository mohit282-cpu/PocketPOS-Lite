package com.example.pocketpos_lite.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Products : Screen("products")
    object POS : Screen("pos")
    object Customers : Screen("customers")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object BusinessProfile : Screen("business_profile")
    object AddProduct : Screen("add_product")
    object EditProduct : Screen("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }
    object Categories : Screen("categories")
}
