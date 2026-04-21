package com.example.invendiario

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.transportesmx.ui.theme.TransportesMXTheme
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        enableEdgeToEdge()
        setContent {
            TransportesMXTheme {
                AppNavigation()
            }
        }
    }
}

// Modelos de datos
data class UserData(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "",
    val empresaId: String = "",
    val name: String = ""
)

data class UnitData(
    val id: String = "",
    val numeroEconomico: String = "",
    val placas: String = "",
    val empresaId: String = "",
    val lastLat: Double = 0.0,
    val lastLng: Double = 0.0
)

data class RouteData(
    val id: String = "",
    val empresaId: String = "",
    val unitId: String = "",
    val unitEco: String = "",
    val cliente: String = "",
    val origenName: String = "",
    val destinoName: String = "",
    val origenLat: Double = 0.0,
    val origenLng: Double = 0.0,
    val destinoLat: Double = 0.0,
    val destinoLng: Double = 0.0,
    val fecha: String = "",
    val horaInicio: String = "",
    val horaFin: String = "",
    val status: String = "pendiente"
)

data class FuelData(
    val id: String = "",
    val empresaId: String = "",
    val unitId: String = "",
    val unitEco: String = "",
    val cantidad: String = "",
    val precio: String = "",
    val total: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val fecha: String = ""
)

@Composable
fun AppNavigation() {
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified ?: false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val db = Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference
            db.child("users").child(currentUser!!.uid).get().addOnSuccessListener { snapshot ->
                val data = snapshot.getValue(UserData::class.java)
                userData = data
                isEmailVerified = if (data?.role == "Gerente") currentUser!!.isEmailVerified else true
            }
        } else {
            userData = null
        }
    }

    if (currentUser == null || (userData?.role == "Gerente" && !isEmailVerified)) {
        if (isRegistering) {
            RegisterScreen(onRegisterSuccess = { currentUser = auth.currentUser; isRegistering = false }, onBackToLogin = { isRegistering = false })
        } else {
            LoginScreen(currentUser = currentUser, isEmailVerified = isEmailVerified, onLoginSuccess = { currentUser = auth.currentUser; currentUser?.reload()?.addOnSuccessListener { isEmailVerified = currentUser?.isEmailVerified ?: false } }, onGoToRegister = { isRegistering = true }, onResendVerification = { currentUser?.sendEmailVerification() }, onSignOut = { auth.signOut(); currentUser = null })
        }
    } else {
        if (userData != null) {
            MainScreen(user = userData!!, onLogout = { auth.signOut(); currentUser = null })
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@Composable
fun LoginScreen(currentUser: com.google.firebase.auth.FirebaseUser?, isEmailVerified: Boolean, onLoginSuccess: () -> Unit, onGoToRegister: () -> Unit, onResendVerification: () -> Unit, onSignOut: () -> Unit) {
    var emailOrUser by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    if (showForgotDialog) ForgotPasswordDialog(onDismiss = { showForgotDialog = false })

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Text("TransportesMX", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            if (currentUser != null && !isEmailVerified) {
                Text("Verifica tu correo: ${currentUser.email}", color = MaterialTheme.colorScheme.error)
                Button(onClick = onResendVerification) { Text("Reenviar verificación") }
                TextButton(onClick = onLoginSuccess) { Text("Ya verifiqué") }
                TextButton(onClick = onSignOut) { Text("Cerrar Sesión") }
            } else {
                OutlinedTextField(value = emailOrUser, onValueChange = { emailOrUser = it }, label = { Text("Correo o Usuario") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showForgotDialog = true }, modifier = Modifier.align(Alignment.End)) { Text("¿Olvidaste tu contraseña?") }
                
                Button(onClick = {
                    if (emailOrUser.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        val finalEmail = if (emailOrUser.contains("@")) emailOrUser else "$emailOrUser@internal.transportesmx.com"
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(finalEmail, password).addOnSuccessListener { onLoginSuccess() }.addOnFailureListener { isLoading = false; Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show() }
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Iniciar Sesión")
                }
                TextButton(onClick = onGoToRegister) { Text("Regístrate como Gerente") }
            }
        }
    }
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Recuperar") }, text = { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnSuccessListener { onDismiss() } }) { Text("Enviar") } })
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var empresaId by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Nueva Empresa", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = empresaId, onValueChange = { empresaId = it }, label = { Text("ID Empresa") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnSuccessListener { res ->
                    val userData = UserData(uid = res.user!!.uid, email = email, role = "Gerente", empresaId = empresaId)
                    Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference.child("users").child(res.user!!.uid).setValue(userData).addOnSuccessListener { onRegisterSuccess() }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Registrar") }
            TextButton(onClick = onBackToLogin) { Text("Volver") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(user: UserData, onLogout: () -> Unit) {
    var currentSubScreen by remember { mutableStateOf("dashboard") }
    var selectedTab by remember { mutableIntStateOf(if (user.role == "Conductor") 0 else 1) } 

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { 
                    Column { 
                        Text(user.empresaId, fontWeight = FontWeight.Bold) 
                        val detailText = if (user.role == "Gerente") user.email else user.username
                        Text("${user.role} | $detailText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) 
                    } 
                }, 
                actions = { IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, null) } }
            ) 
        },
        bottomBar = {
            if (currentSubScreen == "dashboard") {
                NavigationBar {
                    if (user.role == "Gerente" || user.role == "Coordinador") {
                        NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("Usuarios") })
                        NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.LocalShipping, null) }, label = { Text("Unidades") })
                        if (user.role == "Gerente") {
                            NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Métricas") })
                        }
                    } else if (user.role == "Conductor") {
                        NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Route, null) }, label = { Text("Viaje") })
                        NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.LocalGasStation, null) }, label = { Text("Consumible") })
                    }
                }
            }
        }
    ) { 
        Box(modifier = Modifier.padding(it)) {
            when (currentSubScreen) {
                "dashboard" -> {
                    when (user.role) {
                        "Gerente", "Coordinador" -> ManagerDashboardRefactored(user, selectedTab, onViewRoutes = { currentSubScreen = "routes" })
                        "Conductor" -> DriverDashboard(user, selectedTab)
                    }
                }
                "routes" -> RoutesListScreen(user.empresaId, onBack = { currentSubScreen = "dashboard" })
            }
        }
    }
}

@Composable
fun ManagerDashboardRefactored(currentUser: UserData, selectedTab: Int, onViewRoutes: () -> Unit) {
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showCreateUnitDialog by remember { mutableStateOf(false) }
    var showCreateRouteDialog by remember { mutableStateOf(false) }
    var usersList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var unitsList by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var routesList by remember { mutableStateOf<List<RouteData>>(emptyList()) }
    var fuelList by remember { mutableStateOf<List<FuelData>>(emptyList()) }
    
    val db = Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference

    LaunchedEffect(currentUser.empresaId) {
        db.child("users").orderByChild("empresaId").equalTo(currentUser.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { usersList = s.children.mapNotNull { it.getValue(UserData::class.java) }.filter { it.role != "Gerente" } }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("unidades").orderByChild("empresaId").equalTo(currentUser.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { unitsList = s.children.mapNotNull { it.getValue(UnitData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("rutas").orderByChild("empresaId").equalTo(currentUser.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { routesList = s.children.mapNotNull { it.getValue(RouteData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("fuel").orderByChild("empresaId").equalTo(currentUser.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { fuelList = s.children.mapNotNull { it.getValue(FuelData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (selectedTab) {
            0 -> {
                Button(onClick = { showCreateUserDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Crear Usuario") }
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) { items(usersList) { user -> UserItem(user, { db.child("users").child(user.uid).removeValue() }, { db.child("users").child(user.uid).child("role").setValue(it) }) } }
            }
            1 -> {
                // Construir puntos de origen por unidad a partir de sus rutas asignadas
                val unitsWithOrigin = unitsList.mapNotNull { unit ->
                    val ruta = routesList.firstOrNull { it.unitId == unit.id && it.origenLat != 0.0 }
                    if (ruta != null) unit.copy(lastLat = ruta.origenLat, lastLng = ruta.origenLng) else null
                }
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RectangleShape).background(Color.DarkGray)) {
                    OSMView(Modifier.fillMaxSize(), units = unitsWithOrigin)
                }
                if (unitsWithOrigin.isEmpty() && unitsList.isNotEmpty()) {
                    Text(
                        "Las unidades aparecerán en el mapa al asignarles una ruta con origen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showCreateUnitDialog = true }, modifier = Modifier.weight(1f)) { Text("Nueva Unidad") }
                    Button(onClick = { showCreateRouteDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Asignar Ruta") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onViewRoutes, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.List, null); Spacer(Modifier.width(8.dp)); Text("Ver Viajes Asignados")
                }
                LazyColumn(modifier = Modifier.weight(1f)) { items(unitsList) { unit -> UnitItem(unit, currentUser.empresaId, { db.child("unidades").child(unit.id).removeValue() }) } }
            }
            2 -> MetricsScreen(routesList, unitsList, fuelList)
        }

        if (showCreateUserDialog) CreateUserDialog(currentUser, { showCreateUserDialog = false })
        if (showCreateUnitDialog) CreateUnitDialog(currentUser.empresaId, { showCreateUnitDialog = false })
        if (showCreateRouteDialog) CreateRouteDialog(currentUser.empresaId, unitsList, { showCreateRouteDialog = false })
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DriverDashboard(user: UserData, selectedTab: Int) {
    var unitsList by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var selectedUnit by remember { mutableStateOf<UnitData?>(null) }
    var assignedRoutes by remember { mutableStateOf<List<RouteData>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<RouteData?>(null) }
    val db = Firebase.database("https://invendiario-default-rtdb.firebaseio.com/").reference
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(user.empresaId) {
        db.child("unidades").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { unitsList = s.children.mapNotNull { it.getValue(UnitData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    LaunchedEffect(selectedUnit) {
        if (selectedUnit != null) {
            db.child("rutas").orderByChild("unitId").equalTo(selectedUnit!!.id).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) { 
                    assignedRoutes = s.children.mapNotNull { it.getValue(RouteData::class.java) }.filter { it.status != "finalizado" }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
            // Enviar posición cada 3 min
            while (true) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                        if (loc != null) {
                            db.child("unidades").child(selectedUnit!!.id).updateChildren(mapOf("lastLat" to loc.latitude, "lastLng" to loc.longitude))
                        }
                    }
                } catch (e: Exception) {}
                delay(180000) 
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedTab == 0) {
            Text("Operación de Viaje", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            UnitSelector(unitsList, selectedUnit) { selectedUnit = it }
            
            if (selectedUnit != null) {
                Spacer(Modifier.height(16.dp))
                RouteSelector(assignedRoutes, selectedRoute) { selectedRoute = it }
                
                if (selectedRoute != null) {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Cliente: ${selectedRoute!!.cliente}", fontWeight = FontWeight.Bold)
                            Text("Ruta: ${selectedRoute!!.origenName} -> ${selectedRoute!!.destinoName}")
                            Text("Fecha: ${selectedRoute!!.fecha}")
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(vertical = 16.dp).clip(RoundedCornerShape(8.dp))) {
                        OSMView(Modifier.fillMaxSize(), startPoint = GeoPoint(selectedRoute!!.origenLat, selectedRoute!!.origenLng), endPoint = GeoPoint(selectedRoute!!.destinoLat, selectedRoute!!.destinoLng))
                    }
                    Button(onClick = { 
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    db.child("unidades").child(selectedUnit!!.id).updateChildren(mapOf("lastLat" to loc.latitude, "lastLng" to loc.longitude))
                                    Toast.makeText(context, "Ubicación actualizada correctamente", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {}
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.MyLocation, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar Ubicación Actual") 
                    }
                }
            }
        } else {
            FuelScreen(user.empresaId, selectedUnit, unitsList) { selectedUnit = it }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun FuelScreen(empresaId: String, selectedUnit: UnitData?, units: List<UnitData>, onUnitSelect: (UnitData) -> Unit) {
    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Reporte de Consumo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        UnitSelector(units, selectedUnit, onUnitSelect)
        
        if (selectedUnit != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = cantidad, onValueChange = { cantidad = it }, label = { Text("Litros Cargados") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = precio, onValueChange = { precio = it }, label = { Text("Precio por Litro") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = total, onValueChange = { total = it }, label = { Text("Total Pagado ($)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                if (cantidad.isNotBlank() && total.isNotBlank()) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        val db = Firebase.database.reference
                        val id = db.child("fuel").push().key ?: ""
                        val fuel = FuelData(id, empresaId, selectedUnit.id, selectedUnit.numeroEconomico, cantidad, precio, total, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0, SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
                        db.child("fuel").child(id).setValue(fuel).addOnSuccessListener {
                            Toast.makeText(context, "Carga de diesel registrada", Toast.LENGTH_SHORT).show()
                            cantidad = ""; precio = ""; total = ""
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { 
                Icon(Icons.Default.AddLocation, null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar Carga de Diesel") 
            }
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
            units.forEach { u -> DropdownMenuItem(text = { Text("Eco: ${u.numeroEconomico}") }, onClick = { onSelect(u); expanded = false }) }
        }
    }
}

@Composable
fun RouteSelector(routes: List<RouteData>, selectedRoute: RouteData?, onSelect: (RouteData) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedRoute?.cliente ?: "Seleccione Viaje Asignado",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routes.forEach { r -> DropdownMenuItem(text = { Text("${r.cliente} | ${r.fecha}") }, onClick = { onSelect(r); expanded = false }) }
        }
    }
}

// Geocodificación inversa con Nominatim: coordenadas → nombre legible
suspend fun reverseGeocode(lat: Double, lng: Double): String {
    return try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=$lat&lon=$lng&format=json&accept-language=es"
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.setRequestProperty("User-Agent", "TransportesMX/1.0")
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
        // Intentar obtener nombre en orden de preferencia
        val addr = json.optJSONObject("address")
        val name = json.optString("display_name", "")
        when {
            addr != null -> {
                val road    = addr.optString("road", "")
                val suburb  = addr.optString("suburb", addr.optString("neighbourhood", ""))
                val city    = addr.optString("city", addr.optString("town", addr.optString("municipality", "")))
                listOf(road, suburb, city).filter { it.isNotBlank() }.take(2).joinToString(", ")
            }
            name.isNotBlank() -> name.split(",").take(2).joinToString(",").trim()
            else -> String.format("%.4f, %.4f", lat, lng)
        }
    } catch (e: Exception) {
        String.format("%.4f, %.4f", lat, lng) // fallback a coordenadas si falla
    }
}


suspend fun fetchOsrmRoute(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
    return try {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${start.longitude},${start.latitude};" +
                "${end.longitude},${end.latitude}" +
                "?overview=full&geometries=geojson"
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
        val coords = json.getJSONArray("routes")
            .getJSONObject(0)
            .getJSONObject("geometry")
            .getJSONArray("coordinates")
        (0 until coords.length()).map { i ->
            val pt = coords.getJSONArray(i)
            GeoPoint(pt.getDouble(1), pt.getDouble(0))
        }
    } catch (e: Exception) {
        listOf(start, end) // fallback: línea recta
    }
}

@Composable
fun OSMView(
    modifier: Modifier,
    onPointSelected: ((GeoPoint) -> Unit)? = null,
    startPoint: GeoPoint? = null,
    endPoint: GeoPoint? = null,
    units: List<UnitData> = emptyList()
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    // Guardamos los puntos de ruta calculados por OSRM
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    // Cuando cambian start/end, pedimos la ruta a OSRM en background
    LaunchedEffect(startPoint, endPoint) {
        if (startPoint != null && endPoint != null) {
            routePoints = withContext(Dispatchers.IO) {
                fetchOsrmRoute(startPoint, endPoint)
            }
        } else {
            routePoints = emptyList()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(25.6866, -100.3161))
                if (onPointSelected != null) {
                    val overlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            onPointSelected(p)
                            val marker = Marker(mapView)
                            marker.position = p
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.removeAll { it is Marker && it.title == "Seleccionado" }
                            marker.title = "Seleccionado"
                            mapView.overlays.add(marker)
                            mapView.invalidate()
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    overlays.add(overlay)
                }
            }
        },
        update = { map ->
            map.overlays.removeAll { it is Marker || it is Polyline }

            // Pintar unidades (marcador azul marino en origen de su ruta)
            units.forEach { unit ->
                if (unit.lastLat != 0.0) {
                    val m = Marker(map)
                    m.position = GeoPoint(unit.lastLat, unit.lastLng)
                    m.title = "Eco: ${unit.numeroEconomico}"
                    val d = map.context.getDrawable(org.osmdroid.library.R.drawable.marker_default)?.mutate()
                    d?.setTint(android.graphics.Color.parseColor("#000080"))
                    m.icon = d
                    map.overlays.add(m)
                }
            }

            // Pintar ruta por calles (o línea recta si OSRM no respondió aún)
            if (startPoint != null && endPoint != null) {
                val pts = routePoints.ifEmpty { listOf(startPoint, endPoint) }

                val line = Polyline()
                line.setPoints(pts)
                line.color = android.graphics.Color.parseColor("#1565C0") // azul oscuro
                line.width = 8f
                map.overlays.add(line)

                // Marcador Origen (azul marino)
                val sM = Marker(map); sM.position = startPoint; sM.title = "Origen"
                val sD = map.context.getDrawable(org.osmdroid.library.R.drawable.marker_default)?.mutate()
                sD?.setTint(android.graphics.Color.parseColor("#000080")); sM.icon = sD
                map.overlays.add(sM)

                // Marcador Destino (rojo)
                val eM = Marker(map); eM.position = endPoint; eM.title = "Destino"
                val eD = map.context.getDrawable(org.osmdroid.library.R.drawable.marker_default)?.mutate()
                eD?.setTint(android.graphics.Color.RED); eM.icon = eD
                map.overlays.add(eM)

                map.post {
                    map.zoomToBoundingBox(
                        org.osmdroid.util.BoundingBox.fromGeoPoints(pts), true, 150
                    )
                }
            }
            map.invalidate()
        },
        modifier = modifier
    )
}

@Composable
fun MetricsScreen(routes: List<RouteData>, units: List<UnitData>, fuelList: List<FuelData>) {
    var startDate by remember { mutableStateOf("") }
    var endDate   by remember { mutableStateOf("") }
    val context  = LocalContext.current
    val calendar = Calendar.getInstance()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        Text("Métricas de Flota", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        // ── Filtro de fechas ──────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { DatePickerDialog(context, { _, y, m, d -> startDate = String.format("%04d-%02d-%02d", y, m + 1, d) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() },
                modifier = Modifier.weight(1f)
            ) { Text(if (startDate.isEmpty()) "Inicio" else startDate) }
            OutlinedButton(
                onClick = { DatePickerDialog(context, { _, y, m, d -> endDate = String.format("%04d-%02d-%02d", y, m + 1, d) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() },
                modifier = Modifier.weight(1f)
            ) { Text(if (endDate.isEmpty()) "Fin" else endDate) }
        }

        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            val filteredRoutes = routes.filter { it.fecha in startDate..endDate }
            val filteredFuel   = fuelList.filter { it.fecha.take(10) in startDate..endDate }

            Spacer(Modifier.height(16.dp))

            // ── Tarjeta: Total viajes ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Viajes")
                    Text("${filteredRoutes.size}", fontSize = 48.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Unidades activas (con al menos 1 ruta en el periodo) ──────────
            val activeUnitIds = filteredRoutes.map { it.unitId }.toSet()
            val activeUnits   = units.filter { it.id in activeUnitIds }

            Text("Unidades Activas (${activeUnits.size} / ${units.size})", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (activeUnits.isEmpty()) {
                Text("Sin actividad en el periodo seleccionado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                activeUnits.forEach { unit ->
                    val viajesUnidad = filteredRoutes.count { it.unitId == unit.id }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalShipping, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Eco ${unit.numeroEconomico}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("$viajesUnidad viaje${if (viajesUnidad != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Combustible por unidad ────────────────────────────────────────
            Text("Gasto de Combustible por Unidad", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (filteredFuel.isEmpty()) {
                Text("Sin registros de combustible en el periodo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Agrupar por unidad
                val fuelByUnit = filteredFuel.groupBy { it.unitId }
                val maxTotal = fuelByUnit.values.maxOfOrNull { g -> g.sumOf { it.total.toDoubleOrNull() ?: 0.0 } }?.coerceAtLeast(1.0) ?: 1.0

                // Totales globales
                val totalLitros = filteredFuel.sumOf { it.cantidad.toDoubleOrNull() ?: 0.0 }
                val totalGasto  = filteredFuel.sumOf { it.total.toDoubleOrNull() ?: 0.0 }

                // Resumen global
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Litros", style = MaterialTheme.typography.bodySmall)
                            Text(String.format("%.1f L", totalLitros), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Gastado", style = MaterialTheme.typography.bodySmall)
                            Text(String.format("$%.2f", totalGasto), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Detalle por unidad con barra proporcional
                fuelByUnit.forEach { (unitId, registros) ->
                    val unitEco   = registros.first().unitEco
                    val litros    = registros.sumOf { it.cantidad.toDoubleOrNull() ?: 0.0 }
                    val gasto     = registros.sumOf { it.total.toDoubleOrNull() ?: 0.0 }
                    val proporcion = (gasto / maxTotal).toFloat().coerceIn(0f, 1f)
                    val cargas    = registros.size

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Eco $unitEco", fontWeight = FontWeight.Medium)
                            Text(String.format("$%.2f · %.1f L · %d carga%s", gasto, litros, cargas, if (cargas != 1) "s" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(proporcion).height(10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Gráfico de actividad ──────────────────────────────────────────
            Text("Gráfico de Actividad", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val isSingle = startDate == endDate
            val data = if (isSingle)
                (0..23).associate { String.format("%02d:00", it) to filteredRoutes.count { r -> r.horaInicio.startsWith(String.format("%02d:", it)) } }
            else
                generateDateRange(startDate, endDate).associateWith { d -> filteredRoutes.count { r -> r.fecha == d } }

            Box(modifier = Modifier.fillMaxWidth().height(200.dp).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Row(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Bottom) {
                    val max = (data.values.maxOrNull()?.coerceAtLeast(1) ?: 1).toFloat()
                    data.forEach { (k, v) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Box(modifier = Modifier.width(30.dp).height((v / max * 140).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                            Text(k, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

fun generateDateRange(start: String, end: String): List<String> {
    val dates = mutableListOf<String>(); val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); val cal = Calendar.getInstance()
    try { cal.time = sdf.parse(start)!!; val endD = sdf.parse(end)!!; while (!cal.time.after(endD)) { dates.add(sdf.format(cal.time)); cal.add(Calendar.DATE, 1) } } catch (e: Exception) {}
    return dates
}

@Composable
fun CreateRouteDialog(empresaId: String, units: List<UnitData>, onDismiss: () -> Unit) {
    var cliente by remember { mutableStateOf("") }; var origenN by remember { mutableStateOf("") }; var destinoN by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }; var hI by remember { mutableStateOf("") }; var hF by remember { mutableStateOf("") }
    var selectedU by remember { mutableStateOf<UnitData?>(null) }; var pickingL by remember { mutableStateOf<String?>(null) }
    var oP by remember { mutableStateOf<GeoPoint?>(null) }; var dP by remember { mutableStateOf<GeoPoint?>(null) }
    val context = LocalContext.current; val calendar = Calendar.getInstance()

    if (pickingL != null) {
        var isGeocoding by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isGeocoding) pickingL = null },
            title = { Text("Seleccione el $pickingL") },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp).clip(RectangleShape)) {
                        OSMView(Modifier.fillMaxSize(), onPointSelected = { p ->
                            isGeocoding = true
                            CoroutineScope(Dispatchers.Main).launch {
                                val nombre = withContext(Dispatchers.IO) {
                                    reverseGeocode(p.latitude, p.longitude)
                                }
                                if (pickingL == "origen") { oP = p; origenN = nombre }
                                else { dP = p; destinoN = nombre }
                                isGeocoding = false
                            }
                        })
                        if (isGeocoding) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Card { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Obteniendo nombre...")
                                } }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { pickingL = null }, enabled = !isGeocoding) { Text("Confirmar") } }
        )
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Asignar Ruta") }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = cliente, onValueChange = { cliente = it }, label = { Text("Cliente") }, modifier = Modifier.fillMaxWidth())
            UnitSelector(units, selectedU) { selectedU = it }
            OutlinedTextField(value = origenN, onValueChange = { origenN = it }, label = { Text("Origen") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { pickingL = "origen" }) { Icon(Icons.Default.Map, null) } })
            OutlinedTextField(value = destinoN, onValueChange = { destinoN = it }, label = { Text("Destino") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { pickingL = "destino" }) { Icon(Icons.Default.Map, null) } })
            OutlinedTextField(value = fecha, onValueChange = {}, label = { Text("Fecha") }, modifier = Modifier.fillMaxWidth(), readOnly = true, trailingIcon = { IconButton(onClick = { DatePickerDialog(context, { _, y, m, d -> fecha = String.format("%04d-%02d-%02d", y, m + 1, d) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() }) { Icon(Icons.Default.CalendarToday, null) } })
            Row {
                OutlinedTextField(value = hI, onValueChange = {}, label = { Text("Inicio") }, modifier = Modifier.weight(1f), readOnly = true, trailingIcon = { IconButton(onClick = { TimePickerDialog(context, { _, h, m -> hI = String.format("%02d:%02d", h, m) }, 12, 0, true).show() }) { Icon(Icons.Default.AccessTime, null) } })
                OutlinedTextField(value = hF, onValueChange = {}, label = { Text("Fin") }, modifier = Modifier.weight(1f), readOnly = true, trailingIcon = { IconButton(onClick = { TimePickerDialog(context, { _, h, m -> hF = String.format("%02d:%02d", h, m) }, 13, 0, true).show() }) { Icon(Icons.Default.AccessTime, null) } })
            }
        }
    }, confirmButton = { Button(onClick = { if (selectedU != null && fecha.isNotBlank()) { val db = Firebase.database.reference; val id = db.child("rutas").push().key ?: ""; db.child("rutas").child(id).setValue(RouteData(id, empresaId, selectedU!!.id, selectedU!!.numeroEconomico, cliente, origenN, destinoN, oP?.latitude ?: 0.0, oP?.longitude ?: 0.0, dP?.latitude ?: 0.0, dP?.longitude ?: 0.0, fecha, hI, hF)).addOnSuccessListener { onDismiss() } } }) { Text("Asignar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun RoutesListScreen(empresaId: String, onBack: () -> Unit) {
    var routesList by remember { mutableStateOf<List<RouteData>>(emptyList()) }
    var viewRoute   by remember { mutableStateOf<RouteData?>(null) }
    var editRoute   by remember { mutableStateOf<RouteData?>(null) }
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

    // Diálogo: ver ruta en mapa
    if (viewRoute != null) {
        AlertDialog(
            onDismissRequest = { viewRoute = null },
            title = { Text("Ruta: ${viewRoute!!.cliente}") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(400.dp).clip(RectangleShape)) {
                    OSMView(
                        modifier = Modifier.fillMaxSize(),
                        startPoint = GeoPoint(viewRoute!!.origenLat, viewRoute!!.origenLng),
                        endPoint   = GeoPoint(viewRoute!!.destinoLat, viewRoute!!.destinoLng)
                    )
                }
            },
            confirmButton = { Button(onClick = { viewRoute = null }) { Text("Cerrar") } }
        )
    }

    // Diálogo: editar ruta
    if (editRoute != null) {
        EditRouteDialog(route = editRoute!!, onDismiss = { editRoute = null })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Viajes Asignados", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        if (routesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay viajes asignados aún.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(routesList) { r ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Unidad: ${r.unitEco}", fontWeight = FontWeight.Bold)
                                Row {
                                    // Ver en mapa
                                    IconButton(onClick = { viewRoute = r }) {
                                        Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    // Editar
                                    IconButton(onClick = { editRoute = r }) {
                                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary)
                                    }
                                    // Eliminar
                                    IconButton(onClick = { db.child("rutas").child(r.id).removeValue() }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                                    }
                                }
                            }
                            Text("Cliente: ${r.cliente}")
                            Text("${r.origenName}  →  ${r.destinoName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${r.fecha}  |  ${r.horaInicio} – ${r.horaFin}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditRouteDialog(route: RouteData, onDismiss: () -> Unit) {
    var cliente  by remember { mutableStateOf(route.cliente) }
    var origenN  by remember { mutableStateOf(route.origenName) }
    var destinoN by remember { mutableStateOf(route.destinoName) }
    var fecha    by remember { mutableStateOf(route.fecha) }
    var hI       by remember { mutableStateOf(route.horaInicio) }
    var hF       by remember { mutableStateOf(route.horaFin) }
    var oP       by remember { mutableStateOf(if (route.origenLat != 0.0) GeoPoint(route.origenLat, route.origenLng) else null) }
    var dP       by remember { mutableStateOf(if (route.destinoLat != 0.0) GeoPoint(route.destinoLat, route.destinoLng) else null) }
    var pickingL by remember { mutableStateOf<String?>(null) }
    val context  = LocalContext.current
    val calendar = Calendar.getInstance()

    if (pickingL != null) {
        var isGeocoding by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isGeocoding) pickingL = null },
            title = { Text("Seleccione el $pickingL") },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp).clip(RectangleShape)) {
                        OSMView(Modifier.fillMaxSize(), onPointSelected = { p ->
                            isGeocoding = true
                            CoroutineScope(Dispatchers.Main).launch {
                                val nombre = withContext(Dispatchers.IO) {
                                    reverseGeocode(p.latitude, p.longitude)
                                }
                                if (pickingL == "origen") { oP = p; origenN = nombre }
                                else { dP = p; destinoN = nombre }
                                isGeocoding = false
                            }
                        })
                        if (isGeocoding) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Card { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Obteniendo nombre...")
                                } }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { pickingL = null }, enabled = !isGeocoding) { Text("Confirmar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Ruta") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cliente, onValueChange = { cliente = it }, label = { Text("Cliente") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = origenN, onValueChange = { origenN = it },
                    label = { Text("Origen") }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { pickingL = "origen" }) { Icon(Icons.Default.Map, null) } }
                )
                OutlinedTextField(
                    value = destinoN, onValueChange = { destinoN = it },
                    label = { Text("Destino") }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { pickingL = "destino" }) { Icon(Icons.Default.Map, null) } }
                )
                OutlinedTextField(
                    value = fecha, onValueChange = {}, label = { Text("Fecha") },
                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                fecha = String.format("%04d-%02d-%02d", y, m + 1, d)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }) { Icon(Icons.Default.CalendarToday, null) }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hI, onValueChange = {}, label = { Text("Inicio") },
                        modifier = Modifier.weight(1f), readOnly = true,
                        trailingIcon = { IconButton(onClick = { TimePickerDialog(context, { _, h, m -> hI = String.format("%02d:%02d", h, m) }, 12, 0, true).show() }) { Icon(Icons.Default.AccessTime, null) } }
                    )
                    OutlinedTextField(
                        value = hF, onValueChange = {}, label = { Text("Fin") },
                        modifier = Modifier.weight(1f), readOnly = true,
                        trailingIcon = { IconButton(onClick = { TimePickerDialog(context, { _, h, m -> hF = String.format("%02d:%02d", h, m) }, 13, 0, true).show() }) { Icon(Icons.Default.AccessTime, null) } }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val db = Firebase.database.reference
                val updates = mapOf(
                    "cliente"    to cliente,
                    "origenName" to origenN,
                    "destinoName" to destinoN,
                    "origenLat"  to (oP?.latitude ?: route.origenLat),
                    "origenLng"  to (oP?.longitude ?: route.origenLng),
                    "destinoLat" to (dP?.latitude ?: route.destinoLat),
                    "destinoLng" to (dP?.longitude ?: route.destinoLng),
                    "fecha"      to fecha,
                    "horaInicio" to hI,
                    "horaFin"    to hF
                )
                db.child("rutas").child(route.id).updateChildren(updates)
                    .addOnSuccessListener { onDismiss() }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun UserItem(user: UserData, onDelete: () -> Unit, onChangeRole: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(user.username, fontWeight = FontWeight.Bold); Text(user.role) }; IconButton(onClick = { val r = listOf("Conductor", "Coordinador"); onChangeRole(r[(r.indexOf(user.role)+1)%r.size]) }) { Icon(Icons.Default.Edit, null) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) } } }
}

@Composable
fun UnitItem(unit: UnitData, empresaId: String, onDelete: () -> Unit) {
    var showFuelDialog by remember { mutableStateOf(false) }

    if (showFuelDialog) {
        ManagerFuelDialog(unit = unit, empresaId = empresaId, onDismiss = { showFuelDialog = false })
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Eco: ${unit.numeroEconomico}", fontWeight = FontWeight.Bold)
                Text("Placas: ${unit.placas}")
            }
            IconButton(onClick = { showFuelDialog = true }) {
                Icon(Icons.Default.LocalGasStation, contentDescription = "Registrar diesel", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }
    }
}

@Composable
fun ManagerFuelDialog(unit: UnitData, empresaId: String, onDismiss: () -> Unit) {
    var cantidad by remember { mutableStateOf("") }
    var precio   by remember { mutableStateOf("") }
    var total    by remember { mutableStateOf("") }
    val context  = LocalContext.current

    // Calcular total automáticamente cuando cambian litros o precio
    LaunchedEffect(cantidad, precio) {
        val c = cantidad.toDoubleOrNull()
        val p = precio.toDoubleOrNull()
        if (c != null && p != null) total = String.format("%.2f", c * p)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Registrar Diesel")
                    Text("Eco: ${unit.numeroEconomico} · ${unit.placas}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cantidad, onValueChange = { cantidad = it },
                    label = { Text("Litros cargados") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precio, onValueChange = { precio = it },
                    label = { Text("Precio por litro ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = total, onValueChange = { total = it },
                    label = { Text("Total pagado ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "El total se calcula automáticamente, pero puedes editarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = cantidad.isNotBlank() && total.isNotBlank(),
                onClick = {
                    val db = Firebase.database.reference
                    val id = db.child("fuel").push().key ?: ""
                    val fuel = FuelData(
                        id, empresaId, unit.id, unit.numeroEconomico,
                        cantidad, precio, total, 0.0, 0.0,
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    )
                    db.child("fuel").child(id).setValue(fuel).addOnSuccessListener {
                        Toast.makeText(context, "Diesel registrado para ${unit.numeroEconomico}", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            ) { Text("Registrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateUnitDialog(empresaId: String, onDismiss: () -> Unit) {
    var eco by remember { mutableStateOf("") }; var pl by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nueva Unidad") }, text = { Column { OutlinedTextField(value = eco, onValueChange = { eco = it }, label = { Text("Eco") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(value = pl, onValueChange = { pl = it }, label = { Text("Placas") }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { Button(onClick = { val db = Firebase.database.reference; val id = db.child("unidades").push().key ?: ""; db.child("unidades").child(id).setValue(UnitData(id, eco, pl, empresaId)).addOnSuccessListener { onDismiss() } }) { Text("Crear") } })
}

@Composable
fun CreateUserDialog(currentUser: UserData, onDismiss: () -> Unit) {
    var user by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }; var selectedRole by remember { mutableStateOf("Conductor") }; val context = LocalContext.current; var isLoading by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo Usuario") }, text = { Column { OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading); OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Pass") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading); Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selectedRole == "Conductor", { selectedRole = "Conductor" }, enabled = !isLoading); Text("Conductor"); if (currentUser.role == "Gerente") { Spacer(Modifier.width(16.dp)); RadioButton(selectedRole == "Coordinador", { selectedRole = "Coordinador" }, enabled = !isLoading); Text("Coordinador") } }; if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp)) } }, confirmButton = { Button(enabled = !isLoading && user.isNotBlank() && pass.isNotBlank(), onClick = { isLoading = true; val sApp = try { FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "secondary") } catch (e: Exception) { FirebaseApp.getInstance("secondary") }; val sAuth = FirebaseAuth.getInstance(sApp); sAuth.createUserWithEmailAndPassword("$user@internal.transportesmx.com", pass).addOnSuccessListener { res -> val userData = UserData(res.user!!.uid, "", user, selectedRole, currentUser.empresaId); Firebase.database.reference.child("users").child(res.user!!.uid).setValue(userData).addOnSuccessListener { sAuth.signOut(); isLoading = false; onDismiss() } }.addOnFailureListener { isLoading = false; Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() } }) { Text("Crear") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar") } })
}
