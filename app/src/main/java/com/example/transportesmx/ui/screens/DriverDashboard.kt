package com.example.transportesmx.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportesmx.models.FuelData
import com.example.transportesmx.models.RouteData
import com.example.transportesmx.models.UnitData
import com.example.transportesmx.models.UserData
import com.example.transportesmx.ui.components.OSMView
import com.example.transportesmx.ui.components.RouteSelector
import com.example.transportesmx.ui.components.UnitSelector
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun DriverDashboard(user: UserData, selectedTab: Int) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val db = Firebase.database.reference
    
    var units by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var selectedUnit by remember { mutableStateOf<UnitData?>(null) }
    var assignedRoutes by remember { mutableStateOf<List<RouteData>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<RouteData?>(null) }

    LaunchedEffect(user.empresaId) {
        db.child("unidades").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                units = s.children.mapNotNull { it.getValue(UnitData::class.java) }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    LaunchedEffect(selectedUnit) {
        if (selectedUnit != null) {
            db.child("rutas").orderByChild("unitId").equalTo(selectedUnit!!.id).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    assignedRoutes = s.children.mapNotNull { it.getValue(RouteData::class.java) }
                        .filter { it.status != "completado" }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
            
            // Actualización de ubicación en tiempo real
            while (true) {
                fused.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        db.child("unidades").child(selectedUnit!!.id).updateChildren(
                            mapOf(
                                "lastLat" to it.latitude,
                                "lastLng" to it.longitude,
                                "lastUpdate" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                "battery" to (70..100).random()
                            )
                        )
                    }
                }
                kotlinx.coroutines.delay(120000)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedTab == 0) {
            Text("Operación de Viaje", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            UnitSelector(units, selectedUnit) { selectedUnit = it }
            
            if (selectedUnit != null) {
                Spacer(Modifier.height(16.dp))
                RouteSelector(assignedRoutes, selectedRoute) { selectedRoute = it }
                
                if (selectedRoute != null) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("CLIENTE: ${selectedRoute!!.cliente}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("RUTA: ${selectedRoute!!.origenName} -> ${selectedRoute!!.destinoName}", style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                            ) {
                                OSMView(
                                    modifier = Modifier.fillMaxSize(),
                                    startPoint = GeoPoint(selectedRoute!!.origenLat, selectedRoute!!.origenLng),
                                    endPoint = GeoPoint(selectedRoute!!.destinoLat, selectedRoute!!.destinoLng)
                                )
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            if (selectedRoute!!.status == "pendiente") {
                                Button(
                                    onClick = {
                                        db.child("rutas").child(selectedRoute!!.id).child("status").setValue("en curso")
                                        if (selectedRoute!!.orderId.isNotEmpty()) {
                                            db.child("orders").child(selectedRoute!!.orderId).child("status").setValue("en camino")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Iniciar Navegación")
                                }
                            } else if (selectedRoute!!.status == "en curso") {
                                Button(
                                    onClick = {
                                        db.child("rutas").child(selectedRoute!!.id).child("status").setValue("completado")
                                        if (selectedRoute!!.orderId.isNotEmpty()) {
                                            db.child("orders").child(selectedRoute!!.orderId).child("status").setValue("entregado")
                                        }
                                        db.child("unidades").child(selectedUnit!!.id).child("status").setValue("disponible")
                                        selectedRoute = null
                                        Toast.makeText(context, "Viaje finalizado", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("Finalizar Entrega")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            DriverFuelForm(user.empresaId, selectedUnit, units) { selectedUnit = it }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DriverFuelForm(empresaId: String, selectedUnit: UnitData?, units: List<UnitData>, onSelect: (UnitData) -> Unit) {
    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Reporte de Diesel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        UnitSelector(units, selectedUnit, onSelect)
        
        if (selectedUnit != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = cantidad, onValueChange = { cantidad = it }, label = { Text("Litros") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = precio, onValueChange = { precio = it }, label = { Text("Precio por Litro") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = total, onValueChange = { total = it }, label = { Text("Total Pagado $") }, modifier = Modifier.fillMaxWidth())
            
            Button(onClick = {
                if (cantidad.isNotBlank() && total.isNotBlank()) {
                    fused.lastLocation.addOnSuccessListener { loc ->
                        val db = Firebase.database.reference
                        val id = db.child("fuel").push().key ?: ""
                        val fuel = FuelData(
                            id = id,
                            empresaId = empresaId,
                            unitId = selectedUnit.id,
                            unitEco = selectedUnit.numeroEconomico,
                            cantidad = cantidad,
                            precio = precio,
                            total = total,
                            lat = loc?.latitude ?: 0.0,
                            lng = loc?.longitude ?: 0.0,
                            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        )
                        db.child("fuel").child(id).setValue(fuel).addOnSuccessListener { 
                            Toast.makeText(context, "Registro guardado", Toast.LENGTH_SHORT).show()
                            cantidad = ""; precio = ""; total = ""
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Registrar Gasto") }
        }
    }
}
