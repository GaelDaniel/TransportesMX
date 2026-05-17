package com.example.transportesmx

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Location
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
    val status: String = "pendiente"
)

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

@Composable
fun AppNavigation() {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified ?: false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isLoading = true
            val db = Firebase.database.reference
            db.child("users").child(currentUser!!.uid).get()
                .addOnSuccessListener { snapshot ->
                    val data = snapshot.getValue(UserData::class.java)
                    if (data != null) {
                        userData = data
                        isEmailVerified = if (data.role == "Gerente") currentUser!!.isEmailVerified else true
                    } else {
                        Log.e("Auth", "Perfil no encontrado en DB para: ${currentUser!!.uid}")
                        auth.signOut()
                        currentUser = null
                        Toast.makeText(context, "Error: Perfil no encontrado", Toast.LENGTH_LONG).show()
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("Auth", "Error al cargar datos: ${e.message}")
                    isLoading = false
                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
        } else {
            userData = null
        }
    }

    if (currentUser == null || (userData != null && userData?.role == "Gerente" && !isEmailVerified)) {
        if (isRegistering) {
            RegisterScreen(onRegisterSuccess = { currentUser = auth.currentUser; isRegistering = false }, onBackToLogin = { isRegistering = false })
        } else {
            LoginScreen(currentUser = currentUser, isEmailVerified = isEmailVerified, onLoginSuccess = { currentUser = auth.currentUser; currentUser?.reload()?.addOnSuccessListener { isEmailVerified = currentUser?.isEmailVerified ?: false } }, onGoToRegister = { isRegistering = true }, onResendVerification = { currentUser?.sendEmailVerification() }, onSignOut = { auth.signOut(); currentUser = null })
        }
    } else {
        if (userData != null) {
            MainScreen(user = userData!!, onLogout = { auth.signOut(); currentUser = null })
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    if (!isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { auth.signOut(); currentUser = null }) {
                            Text("Cancelar Carga / Salir")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(currentUser: com.google.firebase.auth.FirebaseUser?, isEmailVerified: Boolean, onLoginSuccess: () -> Unit, onGoToRegister: () -> Unit, onResendVerification: () -> Unit, onSignOut: () -> Unit) {
    var emailOrUser by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Text("TransportesMX", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            if (currentUser != null && !isEmailVerified) {
                Text("Verifica tu correo: ${currentUser.email}", color = MaterialTheme.colorScheme.error)
                Button(onClick = onResendVerification, modifier = Modifier.fillMaxWidth()) { Text("Reenviar verificación") }
                TextButton(onClick = onLoginSuccess) { Text("Ya verifiqué") }
                TextButton(onClick = onSignOut) { Text("Cerrar Sesión") }
            } else {
                OutlinedTextField(value = emailOrUser, onValueChange = { emailOrUser = it }, label = { Text("Correo o Usuario") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    if (emailOrUser.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        val finalEmail = if (emailOrUser.contains("@")) emailOrUser else "$emailOrUser@internal.transportesmx.com"
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(finalEmail, password)
                            .addOnSuccessListener { onLoginSuccess() }
                            .addOnFailureListener { isLoading = false; Toast.makeText(context, "Credenciales incorrectas", Toast.LENGTH_SHORT).show() }
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Iniciar Sesión")
                }
                TextButton(onClick = onGoToRegister) { Text("Registrar nueva empresa") }
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var empresaId by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Nueva Empresa", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = empresaId, onValueChange = { empresaId = it }, label = { Text("ID Empresa") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Administrador") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if(email.isNotBlank() && password.length >= 6) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnSuccessListener { res ->
                        val userData = UserData(uid = res.user!!.uid, email = email, role = "Gerente", empresaId = empresaId)
                        Firebase.database.reference.child("users").child(res.user!!.uid).setValue(userData).addOnSuccessListener { onRegisterSuccess() }
                    }
                } else {
                    Toast.makeText(FirebaseApp.getInstance().applicationContext, "Datos inválidos (Contraseña min 6 caracteres)", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Registrar") }
            TextButton(onClick = onBackToLogin) { Text("Regresar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(user: UserData, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(if (user.role == "Conductor") 0 else 1) } 
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Column { Text(user.empresaId, fontWeight = FontWeight.Bold); Text("${user.role} | ${user.username}", style = MaterialTheme.typography.bodySmall) } }, 
                actions = { IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) } }
            ) 
        },
        bottomBar = {
            NavigationBar {
                if (user.role == "Gerente" || user.role == "Coordinador") {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.People, null) }, label = { Text("Personal") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.LocalShipping, null) }, label = { Text("Unidades") })
                } else {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Route, null) }, label = { Text("Mi Viaje") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.LocalGasStation, null) }, label = { Text("Diesel") })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (user.role == "Conductor") DriverDashboard(user, selectedTab)
            else ManagerDashboard(user, selectedTab)
        }
    }
}

@Composable
fun ManagerDashboard(user: UserData, selectedTab: Int) {
    val db = Firebase.database.reference
    var units by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var operators by remember { mutableStateOf<List<UserData>>(emptyList()) }

    LaunchedEffect(user.empresaId) {
        db.child("unidades").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { units = s.children.mapNotNull { it.getValue(UnitData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("users").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { operators = s.children.mapNotNull { it.getValue(UserData::class.java) }.filter { it.role != "Gerente" } }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(Modifier.padding(16.dp)) {
        if (selectedTab == 0) {
            Text("Gestión de Personal", style = MaterialTheme.typography.titleLarge)
            LazyColumn {
                items(operators) { op ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(op.username, fontWeight = FontWeight.Bold); Text(op.role) }
                            IconButton(onClick = { db.child("users").child(op.uid).removeValue() }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        }
                    }
                }
            }
        } else {
            Text("Flota de Unidades", style = MaterialTheme.typography.titleLarge)
            LazyColumn {
                items(units) { unit ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("Eco: ${unit.numeroEconomico}", fontWeight = FontWeight.Bold); Text("Placas: ${unit.placas}") }
                            IconButton(onClick = { db.child("unidades").child(unit.id).removeValue() }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DriverDashboard(user: UserData, selectedTab: Int) {
    val context = LocalContext.current
    val fused = LocationServices.getFusedLocationProviderClient(context)
    val db = Firebase.database.reference
    var units by remember { mutableStateOf<List<UnitData>>(emptyList()) }
    var selectedUnit by remember { mutableStateOf<UnitData?>(null) }

    LaunchedEffect(user.empresaId) {
        db.child("unidades").orderByChild("empresaId").equalTo(user.empresaId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { units = s.children.mapNotNull { it.getValue(UnitData::class.java) } }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(Modifier.padding(16.dp)) {
        if (selectedUnit == null) {
            Text("Seleccione su unidad para iniciar", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(units) { u ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedUnit = u }) {
                        Text(u.numeroEconomico, Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            if (selectedTab == 0) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Operando Unidad: ${selectedUnit!!.numeroEconomico}", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            fused.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    db.child("unidades").child(selectedUnit!!.id).updateChildren(mapOf("lastLat" to loc.latitude, "lastLng" to loc.longitude))
                                    Toast.makeText(context, "Ubicación enviada", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, Modifier.fillMaxWidth()) { Text("Enviar Ubicación Actual") }
                        TextButton(onClick = { selectedUnit = null }, Modifier.align(Alignment.CenterHorizontally)) { Text("Cambiar Unidad") }
                    }
                }
            } else {
                Text("Módulo de Reporte Diesel", style = MaterialTheme.typography.titleMedium)
                // Aquí iría el formulario de combustible
            }
        }
    }
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit) {}
