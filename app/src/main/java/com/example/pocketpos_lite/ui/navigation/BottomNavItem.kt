package com.example.pocketpos_lite.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem("Dashboard", Screen.Dashboard.route, Icons.Default.Dashboard)
    object Products : BottomNavItem("Products", "products", Icons.Default.Inventory)
    object POS : BottomNavItem("POS", "pos", Icons.Default.PointOfSale)
    object Customers : BottomNavItem("Customers", "customers", Icons.Default.People)
    object Reports : BottomNavItem("Reports", "reports", Icons.Default.BarChart)
    object Settings : BottomNavItem("Settings", "settings", Icons.Default.Settings)
}
