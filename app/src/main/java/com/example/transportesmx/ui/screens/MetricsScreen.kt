package com.example.transportesmx.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportesmx.models.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

@Composable
fun MetricsScreen(routes: List<RouteData>, units: List<UnitData>, fuelList: List<FuelData>) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var auditLogs by remember { mutableStateOf<List<ActivityLog>>(emptyList()) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val db = Firebase.database.reference

    LaunchedEffect(Unit) {
        db.child("audit").limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                auditLogs = s.children.mapNotNull { it.getValue(ActivityLog::class.java) }.reversed()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Indicadores y Auditoría", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(16.dp))
        
        // Métricas rápidas
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val totalSpent = fuelList.sumOf { it.total.toDoubleOrNull() ?: 0.0 }
            MetricCard("Gasto Diesel", "$${String.format("%.2f", totalSpent)}", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer)
            
            val activeUnits = routes.filter { it.status == "en curso" }.map { it.unitId }.distinct().size
            MetricCard("Unidades en Ruta", activeUnits.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer)
        }

        Spacer(Modifier.height(24.dp))
        
        // Filtros de Auditoría
        Text("Historial de Actividad", fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> startDate = "$y-${m+1}-$d" }, 2025, 0, 1).show() }, Modifier.weight(1f)) {
                Text(startDate.ifEmpty { "Fecha Inicio" })
            }
            OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> endDate = "$y-${m+1}-$d" }, 2025, 0, 1).show() }, Modifier.weight(1f)) {
                Text(endDate.ifEmpty { "Fecha Fin" })
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(auditLogs.filter { log ->
                if (startDate.isEmpty() || endDate.isEmpty()) true
                else log.fecha.take(10) in startDate..endDate
            }) { log ->
                ListItem(
                    headlineContent = { Text(log.accion) },
                    supportingContent = { Text("${log.usuario} • ${log.fecha}") },
                    leadingContent = { Icon(Icons.Default.History, null) }
                )
                Divider()
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}
