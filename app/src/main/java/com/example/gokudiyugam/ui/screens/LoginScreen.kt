package com.example.gokudiyugam.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    preferenceManager: PreferenceManager,
    onLoginSuccess: (String, UserRole) -> Unit,
    onRequireVerification: (String, String) -> Unit,
    onSignUpClick: () -> Unit,
    onEmailSignInClick: (String, String, (Boolean) -> Unit, (String) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isCheckingEmail by remember { mutableStateOf(false) }
    var isEmailRegistered by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGmailUser by remember { mutableStateOf(false) }

    // Forgot Password States
    var showForgotDialog by remember { mutableStateOf(false) }
    var isRequestingReset by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    
    val adminEmail = "bssbadalpur@gmail.com"
    val adminPassword = "Admin@123"

    // ઓટોમેટિક ઈમેલ ચેક લોજિક
    LaunchedEffect(email) {
        if (email.length > 5 && (email.contains("@") || email == "admin")) {
            isCheckingEmail = true
            delay(1000) 

            if (email == "admin" || email == adminEmail) {
                isEmailRegistered = true
                errorMessage = ""
                isCheckingEmail = false
            } else {
                db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            isEmailRegistered = true
                            errorMessage = ""
                            isGmailUser = email.endsWith("@gmail.com")
                        } else {
                            isEmailRegistered = false
                            if (email.contains("@")) {
                                errorMessage = "Email not found. Please Sign Up."
                            }
                        }
                        isCheckingEmail = false
                    }
                    .addOnFailureListener {
                        isCheckingEmail = false
                    }
            }
        } else {
            isEmailRegistered = false
            errorMessage = ""
        }
    }

    // Forgot Password Request Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Forgot Password?") },
            text = {
                Text("An admin request will be sent for your email: $email\n\nAdmin will send you a reset link once approved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRequestingReset = true
                        db.collection("users").whereEqualTo("email", email).get()
                            .addOnSuccessListener { result ->
                                if (!result.isEmpty) {
                                    val userDoc = result.documents[0]
                                    val uid = userDoc.id
                                    val name = userDoc.getString("name") ?: "Unknown User"

                                    val requestData = mapOf(
                                        "uid" to uid,
                                        "email" to email,
                                        "name" to name,
                                        "status" to "pending",
                                        "timestamp" to System.currentTimeMillis()
                                    )

                                    db.collection("password_requests").document(uid).set(requestData)
                                        .addOnSuccessListener {
                                            isRequestingReset = false
                                            showForgotDialog = false
                                            Toast.makeText(context, "Request sent to Admin!", Toast.LENGTH_LONG).show()
                                        }
                                        .addOnFailureListener {
                                            isRequestingReset = false
                                            Toast.makeText(context, "Request failed. Try again.", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    isRequestingReset = false
                                    Toast.makeText(context, "Email not found!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                isRequestingReset = false
                                Toast.makeText(context, "Error checking email", Toast.LENGTH_SHORT).show()
                            }
                    },
                    enabled = !isRequestingReset
                ) {
                    if (isRequestingReset) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Request")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Gokudiyugam", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(40.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            trailingIcon = {
                                if (isCheckingEmail) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else if (isEmailRegistered) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            isError = errorMessage.isNotEmpty() && !isCheckingEmail
                        )

                        if (errorMessage.isNotEmpty()) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                        }

                        AnimatedVisibility(visible = isEmailRegistered, enter = fadeIn() + expandVertically()) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true
                                )
                                
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    TextButton(onClick = { showForgotDialog = true }) {
                                        Text("Forgot Password?", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (password.isNotEmpty()) {
                                            if ((email == "admin" && password == "admin") || (email == adminEmail && password == adminPassword)) {
                                                onLoginSuccess("Admin", UserRole.HOST)
                                            } else {
                                                isLoading = true
                                                onEmailSignInClick(email, password, { isLoading = it }, { errorMessage = it })
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Sign In", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (isGmailUser && !isEmailRegistered && !isCheckingEmail) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Or connect directly with", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { /* TODO: Trigger Google Login */ },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBox, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Continue with Google", color = Color.Black)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("New here?", color = Color.Gray)
                    TextButton(onClick = onSignUpClick) { Text("Sign Up", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
