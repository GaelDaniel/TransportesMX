package com.example.transportesmx.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.transportesmx.models.UserData
import com.example.transportesmx.ui.screens.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    userData: UserData?,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { /* Manejado por AppRoot */ },
                onGoToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("login") },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("main") {
            userData?.let { user ->
                MainScreen(
                    user = user,
                    onLogout = onLogout,
                    onViewRoutes = { navController.navigate("routes") }
                )
            }
        }
        composable("routes") {
            userData?.let { user ->
                RoutesScreen(
                    empresaId = user.empresaId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        // Ruta de carga por seguridad
        composable("loading") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
