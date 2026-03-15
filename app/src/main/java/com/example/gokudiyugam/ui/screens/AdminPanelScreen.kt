@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    currentUserRole: UserRole?,
    preferenceManager: PreferenceManager,
    onNavigateToMediaLibrary: () -> Unit,
    onBack: () -> Unit
) {
    var currentView by remember { mutableStateOf("main") }
    val isHost = currentUserRole == UserRole.HOST

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(currentView) {
                            "accounts" -> "User Accounts"
                            "password_requests" -> "Password Requests"
                            else -> "Admin Panel"
                        }, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentView != "main") currentView = "main" else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentView) {
                "main" -> {
                    AdminMainMenuView(
                        onAccountsClick = { currentView = "accounts" },
                        onPasswordRequestsClick = { currentView = "password_requests" },
                        onMediaLibraryClick = onNavigateToMediaLibrary,
                        isHost = isHost
                    )
                }
                "accounts" -> {
                    AccountsListView()
                }
                "password_requests" -> {
                    PasswordRequestsView()
                }
            }
        }
    }
}

@Composable
fun AdminMainMenuView(
    onAccountsClick: () -> Unit, 
    onPasswordRequestsClick: () -> Unit,
    onMediaLibraryClick: () -> Unit, 
    isHost: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isHost) {
            AdminMenuCard(
                title = "Accounts",
                icon = Icons.Default.People,
                description = "Manage user roles and permissions",
                onClick = onAccountsClick
            )
            
            AdminMenuCard(
                title = "Password Requests",
                icon = Icons.Default.LockReset,
                description = "View and handle password reset requests",
                onClick = onPasswordRequestsClick
            )
        }

        AdminMenuCard(
            title = "Media Library",
            icon = Icons.Default.PermMedia,
            description = "Manage photos, videos, audio and docs",
            onClick = onMediaLibraryClick
        )
    }
}

@Composable
fun PasswordRequestsView() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var requests by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchRequests() {
        db.collection("password_requests")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                requests = result.documents.mapNotNull { it.data }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        fetchRequests()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending requests.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { request ->
                val email = request["email"] as? String ?: ""
                val name = request["name"] as? String ?: "Unknown"
                val uid = request["uid"] as? String ?: ""
                val timestamp = request["timestamp"] as? Long ?: 0L
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(text = email, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Requested on: $date", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    auth.sendPasswordResetEmail(email)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_SHORT).show()
                                            // Delete request after sending
                                            db.collection("password_requests").document(uid).delete()
                                            fetchRequests()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Send Reset Email", fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    db.collection("password_requests").document(uid).delete()
                                        .addOnSuccessListener { fetchRequests() }
                                },
                                modifier = Modifier.weight(0.5f)
                            ) {
                                Text("Dismiss", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountsListView() {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users").get().addOnSuccessListener { result ->
            users = result.documents.mapNotNull { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = doc.id
                data
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No accounts found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { userMap ->
                val userId = userMap["id"] as? String ?: ""
                val username = userMap["name"] as? String ?: "Unknown"
                val email = userMap["email"] as? String ?: ""
                val roleStr = userMap["role"] as? String ?: "NORMAL"
                val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                
                AccountItemCard(username, email, role, onRoleChange = { newRole ->
                    db.collection("users").document(userId).update("role", newRole.name)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Role updated to ${newRole.name}", Toast.LENGTH_SHORT).show()
                            // Update local state
                            users = users.map { 
                                if (it["id"] == userId) it.toMutableMap().apply { put("role", newRole.name) } else it 
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                })
            }
        }
    }
}

@Composable
fun AccountItemCard(username: String, email: String, role: UserRole, onRoleChange: (UserRole) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (username.isNotEmpty()) username.take(1).uppercase() else "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (email.isNotEmpty()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = when(role) {
                        UserRole.HOST -> MaterialTheme.colorScheme.errorContainer
                        UserRole.SUB_HOST -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = role.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(role) {
                            UserRole.HOST -> MaterialTheme.colorScheme.onErrorContainer
                            UserRole.SUB_HOST -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Change Role")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Make Host") },
                        onClick = {
                            onRoleChange(UserRole.HOST)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Make Normal User") },
                        onClick = {
                            onRoleChange(UserRole.NORMAL)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Make Sub-Host") },
                        onClick = {
                            onRoleChange(UserRole.SUB_HOST)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminMenuCard(title: String, icon: ImageVector, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
