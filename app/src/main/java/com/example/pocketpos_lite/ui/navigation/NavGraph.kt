package com.example.pocketpos_lite.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pocketpos_lite.feature.auth.LoginScreen
import com.example.pocketpos_lite.feature.auth.RegisterScreen
import com.example.pocketpos_lite.feature.dashboard.DashboardScreen
import com.example.pocketpos_lite.feature.startup.SplashScreen
import com.example.pocketpos_lite.ui.components.MainScaffold
import com.example.pocketpos_lite.feature.business.BusinessProfileScreen
import com.example.pocketpos_lite.feature.business.SettingsScreen
import com.example.pocketpos_lite.feature.pos.POSScreen
import com.example.pocketpos_lite.feature.products.ProductScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            MainScaffold(navController) { modifier ->
                DashboardScreen(
                    modifier = modifier,
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable(Screen.Settings.route) {
            MainScaffold(navController) { modifier ->
                SettingsScreen(
                    modifier = modifier,
                    onNavigateToProfile = {
                        navController.navigate(Screen.BusinessProfile.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.BusinessProfile.route) {
            BusinessProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Products.route) {
            MainScaffold(navController) { modifier ->
                ProductScreen(modifier = modifier)
            }
        }
        composable(Screen.POS.route) {
            MainScaffold(navController) { modifier ->
                POSScreen(modifier = modifier)
            }
        }
        composable(Screen.Customers.route) { MainScaffold(navController) { PlaceholderScreen("Customers", it) } }
        composable(Screen.Reports.route) { MainScaffold(navController) { PlaceholderScreen("Reports", it) } }
    }
}

@Composable
fun PlaceholderScreen(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$name Module Coming Soon")
    }
}
