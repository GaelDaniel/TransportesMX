package com.example.transportesmx.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transportesmx.models.RouteData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    empresaId: String,
    onBack: () -> Unit
) {
    var routesList by remember { mutableStateOf<List<RouteData>>(emptyList()) }
    val db = Firebase.database.reference

    LaunchedEffect(empresaId) {
        db.child("rutas").orderByChild("empresaId").equalTo(empresaId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    routesList = s.children.mapNotNull { it.getValue(RouteData::class.java) }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viajes Registrados") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(routesList) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = route.cliente, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Unidad: ${route.unitEco}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Ruta: ${route.origenName} -> ${route.destinoName}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Fecha: ${route.fecha}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Estado: ${route.status}",
                            color = if (route.status == "completado") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
