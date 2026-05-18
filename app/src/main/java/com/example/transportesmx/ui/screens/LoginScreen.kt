package com.example.transportesmx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    var emailOrUser by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Text("TransportesMX", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = emailOrUser,
                onValueChange = { emailOrUser = it },
                label = { Text("Correo o Usuario") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = {
                    if (emailOrUser.contains("@")) {
                        auth.sendPasswordResetEmail(emailOrUser).addOnSuccessListener {
                            Toast.makeText(context, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Ingresa tu correo en el campo de arriba", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("¿Olvidaste tu contraseña?")
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (emailOrUser.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        val email = if (emailOrUser.contains("@")) emailOrUser else "$emailOrUser@internal.transportesmx.com"
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { isLoading = false; onLoginSuccess() }
                            .addOnFailureListener { isLoading = false; Toast.makeText(context, "Error de acceso", Toast.LENGTH_SHORT).show() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Entrar")
            }
            TextButton(onClick = onGoToRegister) { Text("Registrarse como Empresa o Cliente") }
        }
    }
}
