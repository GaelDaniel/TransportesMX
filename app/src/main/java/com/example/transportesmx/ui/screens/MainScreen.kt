package com.example.transportesmx.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transportesmx.models.UserData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: UserData,
    onLogout: () -> Unit,
    onViewRoutes: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(user.empresaId, fontWeight = FontWeight.Bold)
                        Text("${user.role} | ${user.username}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                when (user.role) {
                    "Gerente", "Coordinador" -> {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Monitor, null) },
                            label = { Text("Monitor") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Route, null) },
                            label = { Text("Rutas") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.BarChart, null) },
                            label = { Text("Métricas") }
                        )
                        if (user.role == "Gerente") {
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.ManageAccounts, null) },
                                label = { Text("Usuarios") }
                            )
                        }
                    }
                    "Conductor" -> {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Navigation, null) },
                            label = { Text("Mi Viaje") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.LocalGasStation, null) },
                            label = { Text("Diesel") }
                        )
                    }
                    "Cliente" -> {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.AddBox, null) },
                            label = { Text("Nuevo Pedido") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Assignment, null) },
                            label = { Text("Mis Pedidos") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (user.role) {
                "Gerente", "Coordinador" -> ManagerDashboard(user, selectedTab)
                "Conductor" -> DriverDashboard(user, selectedTab)
                "Cliente" -> CustomerDashboard(user, selectedTab)
            }
        }
    }
}
