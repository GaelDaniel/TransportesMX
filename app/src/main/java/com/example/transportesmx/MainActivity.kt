package com.example.transportesmx

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.transportesmx.models.UserData
import com.example.transportesmx.ui.navigation.AppNavGraph
import com.example.transportesmx.ui.theme.TransportesMXTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        enableEdgeToEdge()
        setContent {
            TransportesMXTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val navController = rememberNavController()
    
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isInitializing by remember { mutableStateOf(true) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { currentUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isInitializing = true
            val db = FirebaseDatabase.getInstance("https://invendiario-default-rtdb.firebaseio.com/").reference
            db.child("users").child(currentUser!!.uid).get()
                .addOnSuccessListener { snapshot ->
                    val data = snapshot.getValue(UserData::class.java)
                    if (data != null) {
                        userData = data
                    } else {
                        auth.signOut()
                        Toast.makeText(context, "Error: Datos de usuario no encontrados", Toast.LENGTH_LONG).show()
                    }
                    isInitializing = false
                }
                .addOnFailureListener {
                    isInitializing = false
                    Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
                }
        } else {
            userData = null
            isInitializing = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (isInitializing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("TransportesMX - Cargando...")
                }
            }
        } else {
            AppNavGraph(
                navController = navController,
                startDestination = if (currentUser != null && userData != null) "main" else "login",
                userData = userData,
                onLogout = {
                    auth.signOut()
                    userData = null
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }
    }
}
