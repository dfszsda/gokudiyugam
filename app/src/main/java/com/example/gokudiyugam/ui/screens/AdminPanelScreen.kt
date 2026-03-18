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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.network.GoogleSheetsUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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
                            "overview" -> "Overview"
                            "home_slider" -> "Manage Home Slider"
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
                        onOverviewClick = { currentView = "overview" },
                        onHomeSliderClick = { currentView = "home_slider" },
                        onMediaLibraryClick = onNavigateToMediaLibrary,
                        isHost = isHost
                    )
                }
                "overview" -> {
                    AdminOverviewView()
                }
                "accounts" -> {
                    AccountsListView(preferenceManager)
                }
                "password_requests" -> {
                    PasswordRequestsView()
                }
                "home_slider" -> {
                    HomeSliderManagementView()
                }
            }
        }
    }
}

@Composable
fun HomeSliderManagementView() {
    val db = FirebaseFirestore.getInstance("mediadata")
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchImages() {
        db.collection("home_images").get().addOnSuccessListener { result ->
            images = result.documents.map { it.id to (it.getString("url") ?: "") }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { fetchImages() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add New Slider Image (URL)", fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter image URL...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (imageUrl.isNotBlank()) {
                    db.collection("home_images").add(mapOf("url" to imageUrl))
                        .addOnSuccessListener {
                            imageUrl = ""
                            fetchImages()
                            Toast.makeText(context, "Image added!", Toast.LENGTH_SHORT).show()
                        }
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(images) { (id, url) ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Image ID: ${id.take(6)}...", modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                db.collection("home_images").document(id).delete()
                                    .addOnSuccessListener { fetchImages() }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOverviewView() {
    val db = FirebaseFirestore.getInstance("mediadata")
    var totalUsers by remember { mutableIntStateOf(0) }
    var onlineUsers by remember { mutableIntStateOf(0) }
    var totalMedia by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            totalUsers = snapshot?.size() ?: 0
            onlineUsers = snapshot?.documents?.count { it.getBoolean("isOnline") == true } ?: 0
        }
        db.collection("mediadata").addSnapshotListener { snapshot, _ ->
            totalMedia = snapshot?.size() ?: 0
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard("Total Accounts", totalUsers.toString(), Icons.Default.People, Color(0xFF6200EE))
        StatCard("Online Now", onlineUsers.toString(), Icons.Default.Circle, Color(0xFF28A745))
        StatCard("Media Items", totalMedia.toString(), Icons.Default.PermMedia, Color(0xFFFFC107))
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
fun AdminMainMenuView(
    onAccountsClick: () -> Unit, 
    onPasswordRequestsClick: () -> Unit,
    onOverviewClick: () -> Unit,
    onHomeSliderClick: () -> Unit,
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
                title = "Overview",
                icon = Icons.Default.Dashboard,
                description = "View app statistics and online users",
                onClick = onOverviewClick
            )

            AdminMenuCard(
                title = "Home Slider",
                icon = Icons.Default.BurstMode,
                description = "Manage images on Home Screen slider",
                onClick = onHomeSliderClick
            )

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
    val db = FirebaseFirestore.getInstance("mediadata")
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
fun AccountsListView(preferenceManager: PreferenceManager) {
    val db = FirebaseFirestore.getInstance("mediadata")
    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchUsers() {
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

    LaunchedEffect(Unit) {
        fetchUsers()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No accounts found.")
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
                val isOnline = userMap["isOnline"] as? Boolean ?: false
                val permissions = (userMap["permissions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val canDelete = userMap["canDelete"] as? Boolean ?: false
                
                AccountItemCard(
                    username = username,
                    email = email,
                    role = role,
                    isOnline = isOnline,
                    permissions = permissions,
                    canDelete = canDelete,
                    onUpdate = { updates ->
                        db.collection("users").document(userId).update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(context, "User updated successfully", Toast.LENGTH_SHORT).show()
                                fetchUsers()
                                
                                if (updates.containsKey("role")) {
                                    lifecycleScope.launch {
                                        val savedPass = preferenceManager.getPasswordForUser(username)
                                        GoogleSheetsUploader.uploadUserData(
                                            email = email,
                                            role = updates["role"] as String,
                                            password = savedPass,
                                            uid = userId
                                        )
                                    }
                                }
                            }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountItemCard(
    username: String, 
    email: String, 
    role: UserRole, 
    isOnline: Boolean,
    permissions: List<String>,
    canDelete: Boolean,
    onUpdate: (Map<String, Any>) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = if (username.isNotEmpty()) username.take(1).uppercase() else "?", color = Color.White)
                        }
                    }
                    if (isOnline) {
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFF28A745), CircleShape).align(Alignment.BottomEnd).background(Color.White, CircleShape).padding(2.dp).background(Color(0xFF28A745), CircleShape))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = username, fontWeight = FontWeight.Bold)
                    Text(text = email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(text = role.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Make Host") }, onClick = { onUpdate(mapOf("role" to UserRole.HOST.name)); showMenu = false })
                        DropdownMenuItem(text = { Text("Make Sub-Host") }, onClick = { onUpdate(mapOf("role" to UserRole.SUB_HOST.name)); showMenu = false })
                        DropdownMenuItem(text = { Text("Make Normal") }, onClick = { onUpdate(mapOf("role" to UserRole.NORMAL.name)); showMenu = false })
                    }
                }
            }

            if (role == UserRole.SUB_HOST) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Permissions:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val availableScreens = listOf("Kirtan", "Sabha", "Functions", "News", "Daily Darshan")
                    availableScreens.forEach { screen ->
                        FilterChip(
                            selected = permissions.contains(screen),
                            onClick = {
                                val newList = if (permissions.contains(screen)) permissions - screen else permissions + screen
                                onUpdate(mapOf("permissions" to newList))
                            },
                            label = { Text(screen, fontSize = 10.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Can Delete Data", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = canDelete,
                    onCheckedChange = { onUpdate(mapOf("canDelete" to it)) }
                )
            }
        }
    }
}

@Composable
fun AdminMenuCard(title: String, icon: ImageVector, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
