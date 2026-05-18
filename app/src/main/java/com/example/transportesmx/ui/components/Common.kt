package com.example.transportesmx.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportesmx.models.*

@Composable
fun StatusChip(status: String) {
    val color = when (status.lowercase()) {
        "solicitado", "pendiente" -> Color(0xFFFFA000)
        "asignado", "en curso" -> Color(0xFF1976D2)
        "entregado", "completado" -> Color(0xFF388E3C)
        "geofence_alerta" -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UnitMonitorItem(unit: UnitData) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocalShipping, 
                contentDescription = null, 
                tint = if(unit.status == "disponible") Color(0xFF2E7D32) else Color(0xFF1565C0),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Eco: ${unit.numeroEconomico}", fontWeight = FontWeight.Bold)
                Text("Bat: ${unit.battery}% • GPS: ${if(unit.lastLat != 0.0) "Activo" else "Buscando..."}", style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(unit.status)
        }
    }
}

@Composable
fun UnitSelector(units: List<UnitData>, selectedUnit: UnitData?, onSelect: (UnitData) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedUnit?.numeroEconomico ?: "Seleccione Unidad",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { u ->
                DropdownMenuItem(
                    text = { Text("Eco: ${u.numeroEconomico}") },
                    onClick = { onSelect(u); expanded = false }
                )
            }
        }
    }
}

@Composable
fun RouteSelector(routes: List<RouteData>, selectedRoute: RouteData?, onSelect: (RouteData) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedRoute?.cliente ?: "Seleccione Viaje",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routes.forEach { r ->
                DropdownMenuItem(
                    text = { Text("${r.cliente} | ${r.fecha}") },
                    onClick = { onSelect(r); expanded = false }
                )
            }
        }
    }
}

@Composable
fun UnitAdminItem(unit: UnitData, onFuel: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Eco: ${unit.numeroEconomico}", fontWeight = FontWeight.Bold)
                Text("Placas: ${unit.placas}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onFuel) { Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}

@Composable
fun RouteAdminItem(route: RouteData, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ruta: ${route.cliente}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusChip(route.status)
            }
            Text("Unidad: ${route.unitEco}", style = MaterialTheme.typography.bodySmall)
            Text("${route.origenName} -> ${route.destinoName}", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
        }
    }
}

@Composable
fun UserAdminItem(user: UserData, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username.ifEmpty { user.name }, fontWeight = FontWeight.Bold)
                Text(user.role, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}
