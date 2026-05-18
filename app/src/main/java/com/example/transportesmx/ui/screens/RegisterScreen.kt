package com.example.transportesmx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.transportesmx.models.UserData
import com.example.transportesmx.models.ActivityLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var empresaId by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Gerente") } 
    
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Registro de Usuario", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Tipo de registro:", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedRole == "Gerente", onClick = { selectedRole = "Gerente" })
                Text("Empresa")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = selectedRole == "Cliente", onClick = { selectedRole = "Cliente" })
                Text("Cliente")
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre Completo o Razón Social") },
                modifier = Modifier.fillMaxWidth()
            )
            
            if (selectedRole == "Gerente") {
                OutlinedTextField(
                    value = empresaId,
                    onValueChange = { empresaId = it },
                    label = { Text("ID Empresa (Ej: TransNorte)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (email.isNotBlank() && password.length >= 6 && name.isNotBlank()) {
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { res ->
                                val finalEmpId = if (selectedRole == "Cliente") "CLIENT_POOL" else empresaId
                                // Se asigna el nombre ingresado al campo username para que aparezca correctamente
                                val userData = UserData(
                                    uid = res.user!!.uid,
                                    email = email,
                                    username = name, 
                                    role = selectedRole,
                                    empresaId = finalEmpId,
                                    name = name
                                )
                                val db = FirebaseDatabase.getInstance("https://invendiario-default-rtdb.firebaseio.com/").reference
                                db.child("users").child(res.user!!.uid).setValue(userData).addOnSuccessListener { 
                                    // Log de Auditoría automática
                                    val logId = db.child("audit").push().key ?: ""
                                    db.child("audit").child(logId).setValue(ActivityLog(
                                        id = logId,
                                        usuario = name,
                                        accion = "Registro inicial de $selectedRole",
                                        fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                        tipo = "info"
                                    ))
                                    onRegisterSuccess() 
                                }
                            }
                            .addOnFailureListener { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
                    } else {
                        Toast.makeText(context, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Registrar") }
            TextButton(onClick = onBackToLogin) { Text("Regresar") }
        }
    }
}
