package com.example.transportesmx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportesmx.models.*
import com.example.transportesmx.ui.components.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManagerDashboard(user: UserData, selectedTab: Int) {
    val db = Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference
    var units by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var routes by remember { mutableStateOf<List<RouteData>>(emptyList()) }
    var orders by remember { mutableStateOf<List<OrderData>>(emptyList()) }
    var fuelLogs by remember { mutableStateOf<List<FuelData>>(emptyList()) }
    var usersList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    
    var showDispatchDialog by remember { mutableStateOf<OrderData?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showFuelDialog by remember { mutableStateOf<UnitData?>(null) }
    var showEditRouteDialog by remember { mutableStateOf<RouteData?>(null) }
    var showEditUserDialog by remember { mutableStateOf<UserData?>(null) }

    LaunchedEffect(user.empresaId) {
        db.child("unidades").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { units = s.children.mapNotNull { it.getValue(UnitData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("rutas").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { routes = s.children.mapNotNull { it.getValue(RouteData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { 
                orders = s.children.mapNotNull { it.getValue(OrderData::class.java) }.filter { it.status == "solicitado" } 
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        if (user.role == "Gerente") {
            db.child("users").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) { usersList = s.children.mapNotNull { it.getValue(UserData::class.java) } }
                override fun onCancelled(e: DatabaseError) {}
            })
        }
        db.child("fuel").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { fuelLogs = s.children.mapNotNull { it.getValue(FuelData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        when (selectedTab) {
            0 -> { // Monitor Pro
                Column(Modifier.fillMaxSize()) {
                    Text("Monitor en Tiempo Real", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Card(modifier = Modifier.fillMaxWidth().height(280.dp).padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        OSMView(Modifier.fillMaxSize(), units = units)
                    }
                    
                    Text("🚨 PEDIDOS ENTRANTES (${orders.size})", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        items(orders) { order ->
                            OrderAlertItem(order) { showDispatchDialog = order }
                        }
                        item { Text("Estado de Flota", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
                        items(units) { unit ->
                            UnitMonitorItem(unit)
                        }
                    }
                }
            }
            1 -> { // Rutas y Flota
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Administración", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = { showUnitDialog = true }) { Text("Añadir Unidad") }
                }
                LazyColumn(Modifier.weight(1f)) {
                    item { Text("Unidades Registradas", modifier = Modifier.padding(vertical = 8.dp)) }
                    items(units) { unit ->
                        UnitAdminItem(unit, onFuel = { showFuelDialog = unit }, onDelete = { db.child("unidades").child(unit.id).removeValue() })
                    }
                    item { Text("Asignaciones", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
                    items(routes) { route ->
                        RouteAdminItem(route, 
                            onDelete = { db.child("rutas").child(route.id).removeValue() },
                            onEdit = { showEditRouteDialog = route }
                        )
                    }
                }
            }
            2 -> MetricsScreen(routes, units, fuelLogs)
            3 -> { 
                if (user.role == "Gerente") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Usuarios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showUserDialog = true }) { Icon(Icons.Default.PersonAdd, null) }
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        items(usersList.filter { it.uid != user.uid }) { u -> 
                            UserAdminItem(
                                user = u, 
                                onEdit = { showEditUserDialog = u }, 
                                onDelete = { db.child("users").child(u.uid).removeValue() }
                            ) 
                        }
                    }
                }
            }
        }
    }

    if (showUserDialog) CreateUserDialog(user) { showUserDialog = false }
    if (showUnitDialog) CreateUnitDialog(user, { showUnitDialog = false })
    if (showFuelDialog != null) ManagerFuelDialog(showFuelDialog!!, user.empresaId, user.username) { showFuelDialog = null }
    if (showDispatchDialog != null) DispatchOrderDialog(showDispatchDialog!!, units, user.username) { showDispatchDialog = null }
    if (showEditRouteDialog != null) EditRouteDialog(showEditRouteDialog!!) { showEditRouteDialog = null }
    if (showEditUserDialog != null) EditUserDialog(showEditUserDialog!!) { showEditUserDialog = null }
}

@Composable
fun OrderAlertItem(order: OrderData, onDispatch: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(order.clienteNombre, fontWeight = FontWeight.Bold)
                Text(order.descripcion, style = MaterialTheme.typography.bodySmall)
                Text("${order.puntoRecogida} ➔ ${order.puntoDestino}", fontSize = 10.sp, color = Color.DarkGray)
            }
            Button(onClick = onDispatch, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Despachar", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ManagerFuelDialog(unit: UnitData, empresaId: String, adminName: String, onDismiss: () -> Unit) {
    var litros by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Diesel - Eco ${unit.numeroEconomico}") },
        text = {
            Column {
                OutlinedTextField(value = litros, onValueChange = { litros = it }, label = { Text("Litros") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = total, onValueChange = { total = it }, label = { Text("Total Pagado $") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (litros.isNotBlank() && total.isNotBlank()) {
                    val db = Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference
                    val id = db.child("fuel").push().key ?: ""
                    val fuel = FuelData(
                        id = id,
                        empresaId = empresaId,
                        unitId = unit.id,
                        unitEco = unit.numeroEconomico,
                        cantidad = litros,
                        precio = "0",
                        total = total,
                        lat = 0.0,
                        lng = 0.0,
                        fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    )
                    db.child("fuel").child(id).setValue(fuel).addOnSuccessListener { 
                        logActivity(adminName, "Registró diesel para Eco ${unit.numeroEconomico}")
                        onDismiss()
                    }
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DispatchOrderDialog(order: OrderData, units: List<UnitData>, adminName: String, onDismiss: () -> Unit) {
    var selectedUnit by remember { mutableStateOf<UnitData?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar Unidad") },
        text = {
            Column {
                Text("Pedido: ${order.descripcion}")
                Spacer(Modifier.height(12.dp))
                UnitSelector(units.filter { it.status == "disponible" }, selectedUnit) { selectedUnit = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedUnit != null) {
                    val db = Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference
                    val routeId = db.child("rutas").push().key ?: ""
                    val newRoute = RouteData(
                        id = routeId,
                        empresaId = selectedUnit!!.empresaId,
                        orderId = order.id,
                        unitId = selectedUnit!!.id,
                        unitEco = selectedUnit!!.numeroEconomico,
                        cliente = order.clienteNombre,
                        origenName = order.puntoRecogida,
                        destinoName = order.puntoDestino,
                        origenLat = order.pickupLat,
                        origenLng = order.pickupLng,
                        destinoLat = order.destinoLat,
                        destinoLng = order.destinoLng,
                        fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        status = "pendiente"
                    )
                    db.child("rutas").child(routeId).setValue(newRoute)
                    db.child("orders").child(order.id).child("status").setValue("asignado")
                    db.child("unidades").child(selectedUnit!!.id).child("status").setValue("en ruta")
                    logActivity(adminName, "Despachó unidad ${selectedUnit!!.numeroEconomico} para pedido ${order.id}")
                    onDismiss()
                }
            }) { Text("Confirmar") }
        }
    )
}

@Composable
fun EditRouteDialog(route: RouteData, onDismiss: () -> Unit) {
    var cliente by remember { mutableStateOf(route.cliente) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Cliente") },
        text = { OutlinedTextField(value = cliente, onValueChange = { cliente = it }, label = { Text("Nombre Cliente") }) },
        confirmButton = { Button(onClick = { 
            Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference.child("rutas").child(route.id).child("cliente").setValue(cliente)
            onDismiss() 
        }) { Text("Guardar") } }
    )
}

@Composable
fun EditUserDialog(userData: UserData, onDismiss: () -> Unit) {
    var role by remember { mutableStateOf(userData.role) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Usuario: ${userData.username}") },
        text = {
            Column {
                Text("Nuevo Rol:")
                Row {
                    RadioButton(role == "Conductor", { role = "Conductor" })
                    Text("Chofer")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(role == "Coordinador", { role = "Coordinador" })
                    Text("Coordinador")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference
                    .child("users").child(userData.uid).child("role").setValue(role)
                onDismiss()
            }) { Text("Actualizar") }
        }
    )
}

fun logActivity(usuario: String, accion: String) {
    val db = Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference
    val id = db.child("audit").push().key ?: ""
    val log = ActivityLog(id, usuario, accion, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
    db.child("audit").child(id).setValue(log)
}
