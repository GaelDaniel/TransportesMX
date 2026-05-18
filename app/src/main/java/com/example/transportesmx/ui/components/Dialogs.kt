package com.example.transportesmx.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.transportesmx.models.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateUnitDialog(currentUser: UserData, onDismiss: () -> Unit) {
    var eco by remember { mutableStateOf("") }
    var pl by remember { mutableStateOf("") }
    val db = FirebaseDatabase.getInstance("https://invendiario-default-rtdb.firebaseio.com/").reference

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Unidad") },
        text = {
            Column {
                OutlinedTextField(value = eco, onValueChange = { eco = it }, label = { Text("Número Económico") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pl, onValueChange = { pl = it }, label = { Text("Placas") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (eco.isNotBlank() && pl.isNotBlank()) {
                    val id = db.child("unidades").push().key ?: ""
                    val unit = UnitData(
                        id = id, 
                        numeroEconomico = eco, 
                        placas = pl, 
                        empresaId = currentUser.empresaId, 
                        status = "disponible",
                        battery = 100,
                        lastUpdate = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    )
                    db.child("unidades").child(id).setValue(unit).addOnSuccessListener {
                        // Registro de Auditoría
                        val logId = db.child("audit").push().key ?: ""
                        db.child("audit").child(logId).setValue(ActivityLog(
                            logId, currentUser.username, "Añadió unidad $eco", 
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        ))
                        onDismiss()
                    }
                }
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CreateUserDialog(currentUser: UserData, onDismiss: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Conductor") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val db = FirebaseDatabase.getInstance("https://invendiario-default-rtdb.firebaseio.com/").reference

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Personal") },
        text = {
            Column {
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(8.dp))
                Text("Asignar Rol:", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedRole == "Conductor", onClick = { selectedRole = "Conductor" })
                    Text("Chofer")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = selectedRole == "Coordinador", onClick = { selectedRole = "Coordinador" })
                    Text("Coordinador")
                }
                
                if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading && user.isNotBlank() && pass.isNotBlank(),
                onClick = {
                    isLoading = true
                    val sApp = try { FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "secondary") } catch (e: Exception) { FirebaseApp.getInstance("secondary") }
                    val sAuth = FirebaseAuth.getInstance(sApp)
                    val email = "$user@internal.transportesmx.com"
                    
                    sAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnSuccessListener { res ->
                            val userData = UserData(res.user!!.uid, email, user, selectedRole, currentUser.empresaId, user)
                            db.child("users").child(res.user!!.uid).setValue(userData)
                                .addOnSuccessListener { 
                                    // Registro de Auditoría
                                    val logId = db.child("audit").push().key ?: ""
                                    db.child("audit").child(logId).setValue(ActivityLog(
                                        logId, currentUser.username, "Creó usuario $user ($selectedRole)", 
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                    ))
                                    sAuth.signOut()
                                    isLoading = false
                                    onDismiss() 
                                }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar") } }
    )
}
