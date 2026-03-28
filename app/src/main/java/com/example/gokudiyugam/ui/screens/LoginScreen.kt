package com.example.gokudiyugam.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    preferenceManager: PreferenceManager,
    onLoginSuccess: (String, UserRole) -> Unit,
    onRequireVerification: (String, String) -> Unit,
    onSignUpClick: () -> Unit,
    onEmailSignInClick: (String, String, (Boolean) -> Unit, (String) -> Unit) -> Unit,
    onGuestLoginClick: ((Boolean) -> Unit, (String) -> Unit) -> Unit,
    onGoogleSignInClick: ((Boolean) -> Unit, (String) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var isRequestingReset by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance("mediadata")

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
                        if (email.isBlank()) {
                            Toast.makeText(context, "Please enter your email first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
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
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
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
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
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
                            onValueChange = { 
                                email = it 
                                errorMessage = "" // Clear error when user types
                            },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            isError = errorMessage.isNotEmpty()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it 
                                errorMessage = "" // Clear error when user types
                            },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            isError = errorMessage.isNotEmpty()
                        )
                        
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(onClick = { showForgotDialog = true }) {
                                Text("Forgot Password?", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter both email and password"
                                } else {
                                    isLoading = true
                                    onEmailSignInClick(email, password, { isLoading = it }, { errorMessage = it })
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Sign In", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text("OR", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isLoading = true
                                    onGoogleSignInClick({ isLoading = it }, { errorMessage = it })
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                enabled = !isLoading
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Google Logo", tint = Color(0xFF4285F4))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Google", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    isLoading = true
                                    onGuestLoginClick({ isLoading = it }, { errorMessage = it })
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isLoading,
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Guest", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
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
