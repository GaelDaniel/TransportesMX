package com.example.transportesmx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transportesmx.models.OrderData
import com.example.transportesmx.models.UserData
import com.example.transportesmx.ui.components.StatusChip
import com.example.transportesmx.utils.GeocodingUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerDashboard(user: UserData, selectedTab: Int) {
    val db = Firebase.database.reference
    var ordersList by remember { mutableStateOf<List<OrderData>>(emptyList()) }
    var showCreateOrderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid) {
        db.child("orders").orderByChild("clienteUid").equalTo(user.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    ordersList = s.children.mapNotNull { it.getValue(OrderData::class.java) }
                        .sortedByDescending { it.fecha }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedTab == 1) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text("Mis Pedidos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                }
                if (ordersList.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aún no tienes pedidos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(ordersList) { order ->
                    OrderCustomerItem(order)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Bienvenido, ${user.username.ifEmpty { user.name }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("¿A dónde enviamos hoy?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { showCreateOrderDialog = true },
                    modifier = Modifier.height(56.dp).fillMaxWidth(0.8f)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nueva Solicitud de Carga")
                }
            }
        }

        if (showCreateOrderDialog) {
            CreateOrderDialog(user = user, onDismiss = { showCreateOrderDialog = false })
        }
    }
}

@Composable
fun OrderCustomerItem(order: OrderData) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Carga #${order.id.takeLast(5)}", fontWeight = FontWeight.Bold)
                StatusChip(order.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(order.descripcion, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color.Red)
                Spacer(Modifier.width(4.dp))
                Text("Origen: ${order.puntoRecogida}", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color(0xFF1565C0))
                Spacer(Modifier.width(4.dp))
                Text("Destino: ${order.puntoDestino}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CreateOrderDialog(user: UserData, onDismiss: () -> Unit) {
    var desc by remember { mutableStateOf("") }
    var pickup by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isValidating) onDismiss() },
        title = { Text("Solicitar Transporte") },
        text = {
            Column {
                if (isValidating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Validando direcciones...", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción de la carga") }, modifier = Modifier.fillMaxWidth(), enabled = !isValidating)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = pickup, onValueChange = { pickup = it }, label = { Text("Dirección de Recogida") }, modifier = Modifier.fillMaxWidth(), enabled = !isValidating)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Dirección de Entrega") }, modifier = Modifier.fillMaxWidth(), enabled = !isValidating)
            }
        },
        confirmButton = {
            Button(
                enabled = desc.isNotBlank() && pickup.isNotBlank() && destination.isNotBlank() && !isValidating,
                onClick = {
                    isValidating = true
                    scope.launch {
                        val pickupPoint = GeocodingUtils.geocodeAddress(pickup)
                        val destPoint = GeocodingUtils.geocodeAddress(destination)
                        
                        if (pickupPoint != null && destPoint != null) {
                            val db = Firebase.database.reference
                            val id = db.child("orders").push().key ?: ""
                            val order = OrderData(
                                id = id,
                                clienteUid = user.uid,
                                clienteNombre = user.username.ifEmpty { user.name },
                                descripcion = desc,
                                puntoRecogida = pickup,
                                puntoDestino = destination,
                                pickupLat = pickupPoint.latitude,
                                pickupLng = pickupPoint.longitude,
                                destinoLat = destPoint.latitude,
                                destinoLng = destPoint.longitude,
                                status = "solicitado",
                                fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            )
                            db.child("orders").child(id).setValue(order).addOnSuccessListener {
                                Toast.makeText(context, "Pedido enviado con éxito", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        } else {
                            isValidating = false
                            Toast.makeText(context, "No se encontró la dirección. Intenta agregar calle y ciudad.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isValidating) { Text("Cancelar") } }
    )
}
