@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.compose.SubcomposeAsyncImage
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.network.GoogleSheetsUploader
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    currentUserRole: UserRole?,
    preferenceManager: PreferenceManager,
    googleAccount: GoogleSignInAccount?,
    onNavigateToMediaLibrary: () -> Unit,
    onBack: () -> Unit
) {
    var currentView by remember { mutableStateOf("main") }
    var selectedUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    val isHost = currentUserRole == UserRole.HOST
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance("mediadata")
    var userPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                userPermissions = (doc?.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when {
                            selectedUser != null -> "User Details"
                            currentView == "accounts" -> "User Accounts"
                            currentView == "password_requests" -> "Password Requests"
                            currentView == "overview" -> "Overview"
                            currentView == "home_slider" -> "Manage Home Slider"
                            currentView == "feedbacks" -> "Help & Feedback"
                            currentView == "lyrics_management" -> "Manage Kirtan Lyrics"
                            else -> "Admin Panel"
                        }, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedUser != null) {
                            selectedUser = null
                        } else if (currentView != "main") {
                            currentView = "main"
                        } else {
                            onBack()
                        }
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
            when {
                selectedUser != null -> {
                    UserDetailView(
                        user = selectedUser!!,
                        isHost = isHost,
                        preferenceManager = preferenceManager,
                        onBack = { selectedUser = null },
                        onUpdate = { updatedUser ->
                            selectedUser = updatedUser
                        }
                    )
                }
                currentView == "main" -> {
                    AdminMainMenuView(
                        onAccountsClick = { currentView = "accounts" },
                        onPasswordRequestsClick = { currentView = "password_requests" },
                        onOverviewClick = { currentView = "overview" },
                        onHomeSliderClick = { currentView = "home_slider" },
                        onFeedbacksClick = { currentView = "feedbacks" },
                        onLyricsManagementClick = { currentView = "lyrics_management" },
                        onMediaLibraryClick = onNavigateToMediaLibrary,
                        currentUserRole = currentUserRole,
                        userPermissions = userPermissions
                    )
                }
                currentView == "overview" -> AdminOverviewView()
                currentView == "accounts" -> {
                    AccountsListView(
                        isHost = isHost,
                        onUserClick = { selectedUser = it }
                    )
                }
                currentView == "password_requests" -> PasswordRequestsView()
                currentView == "home_slider" -> HomeSliderManagementView(isHost || userPermissions.contains("Admin: Home Slider"), googleAccount)
                currentView == "feedbacks" -> AdminFeedbacksView()
                currentView == "lyrics_management" -> AdminLyricsManagementView()
            }
        }
    }
}

@Composable
fun AdminLyricsManagementView() {
    val db = FirebaseFirestore.getInstance("mediadata")
    val context = LocalContext.current
    var allAudios by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pending, 1: Added
    var isLoading by remember { mutableStateOf(true) }
    
    var editingKirtan by remember { mutableStateOf<Map<String, Any>?>(null) }
    var lyricsText by remember { mutableStateOf("") }

    fun fetchAudios() {
        isLoading = true
        db.collection("mediadata")
            .whereEqualTo("mediaType", "audio")
            .get()
            .addOnSuccessListener { result ->
                allAudios = result.documents.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(Unit) { fetchAudios() }

    val filteredAudios = allAudios.filter { audio ->
        val title = audio["title"] as? String ?: ""
        val lyrics = audio["lyrics"] as? String ?: ""
        val matchesSearch = title.lowercase().contains(searchQuery.lowercase())
        val matchesTab = if (selectedTab == 0) lyrics.isBlank() else lyrics.isNotBlank()
        matchesSearch && matchesTab
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search Audio Title...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Box(modifier = Modifier.padding(16.dp)) { Text("Pending Lyrics") }
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Box(modifier = Modifier.padding(16.dp)) { Text("Added Lyrics") }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(filteredAudios) { audio ->
                    val title = audio["title"] as? String ?: "Untitled"
                    val id = audio["id"] as? String ?: ""
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
                            editingKirtan = audio
                            lyricsText = audio["lyrics"] as? String ?: ""
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp), tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (editingKirtan != null) {
        AlertDialog(
            onDismissRequest = { editingKirtan = null },
            title = { Text("Edit Lyrics for ${editingKirtan!!["title"]}") },
            text = {
                OutlinedTextField(
                    value = lyricsText,
                    onValueChange = { lyricsText = it },
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    placeholder = { Text("Paste lyrics here...") },
                    label = { Text("Lyrics (Gujarati/English)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val id = editingKirtan!!["id"] as String
                    db.collection("mediadata").document(id).update("lyrics", lyricsText)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Lyrics updated!", Toast.LENGTH_SHORT).show()
                            fetchAudios()
                            editingKirtan = null
                        }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingKirtan = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AccountsListView(isHost: Boolean, onUserClick: (Map<String, Any>) -> Unit) {
    val db = FirebaseFirestore.getInstance("mediadata")
    val context = LocalContext.current
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchUsers() {
        db.collection("users").get().addOnSuccessListener { result ->
            users = result.documents.map { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = doc.id
                data
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
            Toast.makeText(context, "Failed to load users", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) { fetchUsers() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                AccountItemCard(user = user, onClick = { onUserClick(user) })
            }
        }
    }
}

@Composable
fun AccountItemCard(user: Map<String, Any>, onClick: () -> Unit) {
    val name = user["name"] as? String ?: "Unknown"
    val email = user["email"] as? String ?: ""
    val roleStr = user["role"] as? String ?: "NORMAL"
    val isOnline = user["isOnline"] as? Boolean ?: false
    val lastDevice = user["lastLoginDevice"] as? String ?: "Unknown Device"
    val lastSeen = user["lastSeen"] as? Long ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (name.isNotEmpty()) name.take(1).uppercase() else "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(text = email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Surface(
                    color = when(roleStr) {
                        "HOST" -> Color(0xFFFFEB3B).copy(alpha = 0.2f)
                        "SUB_HOST" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = roleStr,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(roleStr) {
                            "HOST" -> Color(0xFFFBC02D)
                            "SUB_HOST" -> Color(0xFF1976D2)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.alpha(0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = lastDevice, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(
                    text = if (isOnline) "Active Now" else formatTimeAgo(lastSeen),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) Color(0xFF4CAF50) else Color.Gray,
                    fontWeight = if (isOnline) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserDetailView(
    user: Map<String, Any>,
    isHost: Boolean,
    preferenceManager: PreferenceManager,
    onBack: () -> Unit,
    onUpdate: (Map<String, Any>) -> Unit
) {
    val db = FirebaseFirestore.getInstance("mediadata")
    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    
    val userId = user["id"] as? String ?: ""
    val name = user["name"] as? String ?: "Unknown"
    val email = user["email"] as? String ?: ""
    val roleStr = user["role"] as? String ?: "NORMAL"
    val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
    val permissions = (user["permissions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val lastDevice = user["lastLoginDevice"] as? String ?: "Unknown Device"
    val lastSeen = user["lastSeen"] as? Long ?: 0L
    val canDeleteData = user["canDelete"] as? Boolean ?: false

    var isDeleting by remember { mutableStateOf(false) }

    fun updateUserData(updates: Map<String, Any>) {
        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()
                val newUser = user.toMutableMap()
                updates.forEach { (k, v) -> newUser[k] = v }
                onUpdate(newUser)
                
                if (updates.containsKey("role")) {
                    lifecycleScope.launch {
                        // Point 2 Fix: Removed password sync to Google Sheets
                        GoogleSheetsUploader.uploadUserData(email = email, role = updates["role"] as String, uid = userId)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Modern Header
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Info Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailItem(Icons.Default.Devices, "Last Login Device", lastDevice)
                DetailItem(Icons.Default.Schedule, "Last Active", formatTimeAgo(lastSeen))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Role Selection
        Text("Account Role", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserRole.values().filter { it != UserRole.GUEST }.forEach { r ->
                val selected = role == r
                FilterChip(
                    selected = selected,
                    onClick = { if (isHost) updateUserData(mapOf("role" to r.name)) },
                    label = { Text(r.name) },
                    enabled = isHost,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Permissions
        Text("Screen Permissions", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val screens = listOf("Kirtan", "Sabha", "Functions", "News", "Daily Darshan", "Sabha Saar", "Admin: Home Slider", "Admin: Accounts", "Admin: Media Library")
            screens.forEach { screen ->
                val hasPermission = permissions.contains(screen)
                FilterChip(
                    selected = hasPermission,
                    onClick = {
                        if (isHost) {
                            val newList = if (hasPermission) permissions - screen else permissions + screen
                            updateUserData(mapOf("permissions" to newList))
                        }
                    },
                    label = { Text(screen, fontSize = 11.sp) },
                    enabled = isHost
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Allow Data Deletion", fontWeight = FontWeight.Bold)
            Switch(checked = canDeleteData, onCheckedChange = { if (isHost) updateUserData(mapOf("canDelete" to it)) }, enabled = isHost)
        }

        if (isHost) {
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    isDeleting = true
                    db.collection("users").document(userId).delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                        .addOnFailureListener { isDeleting = false }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                enabled = !isDeleting
            ) {
                if (isDeleting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account Permanently")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    if (timestamp == 0L) return "Never Active"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun AdminFeedbacksView() {
    val db = FirebaseFirestore.getInstance("mediadata")
    val context = LocalContext.current
    var feedbacks by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchFeedbacks() {
        db.collection("help_feedback")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                feedbacks = result.documents.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    data
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        fetchFeedbacks()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (feedbacks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No feedback or help requests found.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(feedbacks) { item ->
                val type = item["type"] as? String ?: "Feedback"
                val message = item["message"] as? String ?: ""
                val email = item["email"] as? String ?: ""
                val name = item["userName"] as? String ?: "Guest"
                val status = item["status"] as? String ?: "pending"
                val timestamp = item["timestamp"] as? Long ?: 0L
                val docId = item["docId"] as? String ?: ""
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status == "resolved") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = type, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(text = "From: $name ($email)", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (status == "pending") {
                                Button(
                                    onClick = {
                                        db.collection("help_feedback").document(docId).update("status", "resolved")
                                            .addOnSuccessListener { fetchFeedbacks() }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("Mark Resolved", fontSize = 12.sp)
                                }
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    db.collection("help_feedback").document(docId).delete()
                                        .addOnSuccessListener { fetchFeedbacks() }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSliderManagementView(canEdit: Boolean, googleAccount: GoogleSignInAccount?) {
    val db = FirebaseFirestore.getInstance("mediadata")
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Google Drive Integration
    val driveFolderId = "1_nFilCWknua9FaoDTGQHcHldzDMvBgez"
    val driveHelper = remember(googleAccount) { 
        googleAccount?.let { DriveHelper(DriveHelper.getDriveService(context, it)) }
    }
    
    var imageUrl by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    fun fetchImages() {
        isLoading = true
        fetchImagesFromFirestore(db) { fetchedImages ->
            images = fetchedImages
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { fetchImages() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                if (driveHelper == null) {
                    uploadError = "Please sign in with Google to upload to Drive"
                    return@launch
                }
                isUploading = true
                uploadError = null
                try {
                    // Point: Post in Max Quality (Reduced compression to 100 or use original stream)
                    val photoData = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val outputStream = ByteArrayOutputStream()
                        // Changed from 70 to 100 for Maximum Quality
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        outputStream.toByteArray()
                    }

                    val fileName = "slider_${UUID.randomUUID()}.jpg"
                    
                    val fileId = driveHelper.createFile(fileName, "image/jpeg", photoData.inputStream(), driveFolderId)
                    if (fileId != null) {
                        driveHelper.makeFilePublic(fileId)
                        val (webContentLink, _) = driveHelper.getFileLinks(fileId)
                        
                        // Fallback to construction if link is null
                        val downloadUrl = webContentLink ?: "https://drive.google.com/uc?id=$fileId&export=download"
                        
                        db.collection("home_images").add(mapOf(
                            "url" to downloadUrl, 
                            "driveFileId" to fileId,
                            "timestamp" to System.currentTimeMillis()
                        )).await()
                        
                        fetchImages()
                        Toast.makeText(context, "Photo uploaded to Drive in Max Quality!", Toast.LENGTH_SHORT).show()
                    } else {
                        uploadError = "Failed to upload to Google Drive"
                    }
                } catch (e: Exception) {
                    Log.e("AdminPanel", "Upload failed", e)
                    uploadError = "Upload failed: ${e.localizedMessage}"
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (canEdit) {
            Text("Add New Slider Image (Max Quality)", fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recommended Photo Size (Ratio 16:9):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Pixels: 1280 x 720 px", fontSize = 12.sp)
                    Text("• Inches: 13.33 x 7.5 in", fontSize = 12.sp)
                    Text("• Centimeters: 33.87 x 19.05 cm", fontSize = 12.sp)
                    Text("• Millimeters: 338.7 x 190.5 mm", fontSize = 12.sp)
                    Text("• Points: 960 x 540 pt", fontSize = 12.sp)
                    Text("• Picas: 80 x 45 pc", fontSize = 12.sp)
                }
            }
            
            // Error Display for Uploading
            if (uploadError != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uploadError!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { uploadError = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Paste image URL here...") },
                    label = { Text("Image URL") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (imageUrl.isNotBlank()) {
                        db.collection("home_images").add(mapOf("url" to imageUrl, "timestamp" to System.currentTimeMillis()))
                            .addOnSuccessListener {
                                imageUrl = ""
                                fetchImages()
                                Toast.makeText(context, "URL added!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { uploadError = "Failed to add URL: ${it.localizedMessage}" }
                    }
                }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("OR", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select & Upload Photo (Max Quality)")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
        }

        Text("Current Slider Images", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(images) { imageData ->
                    val id = imageData["id"] as? String ?: ""
                    val url = imageData["url"] as? String ?: ""
                    val driveFileId = imageData["driveFileId"] as? String
                    
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SubcomposeAsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp, 60.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                loading = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp)) } },
                                error = { 
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f))) {
                                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        Text("Load Failed", fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("ID: ${id.take(8)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            if (canEdit) {
                                IconButton(onClick = {
                                    scope.launch {
                                        try {
                                            db.collection("home_images").document(id).delete().await()
                                            
                                            // Delete from Drive if driveFileId exists
                                            if (driveFileId != null && driveHelper != null) {
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        driveHelper.getDriveServiceDirect().files().delete(driveFileId).execute()
                                                    } catch (e: Exception) {
                                                        Log.e("AdminPanel", "Failed to delete from Drive", e)
                                                    }
                                                }
                                            } else if (url.contains("firebasestorage")) {
                                                try { storage.getReferenceFromUrl(url).delete().await() } catch (e: Exception) {}
                                            }
                                            
                                            fetchImages()
                                            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
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
}

private fun fetchImagesFromFirestore(db: FirebaseFirestore, onResult: (List<Map<String, Any>>) -> Unit) {
    db.collection("home_images")
        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .get().addOnSuccessListener { result ->
            val list = result.documents.map { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = doc.id
                data
            }
            onResult(list)
        }
}

@Composable
fun AdminOverviewView() {
    val db = FirebaseFirestore.getInstance("mediadata")
    var totalUsers by remember { mutableStateOf(0) }
    var onlineUsers by remember { mutableStateOf(0) }
    var totalMedia by remember { mutableStateOf(0) }
    var helpCount by remember { mutableStateOf(0) }
    var feedbackCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            totalUsers = snapshot?.size() ?: 0
            onlineUsers = snapshot?.documents?.count { it.getBoolean("isOnline") == true } ?: 0
        }
        db.collection("mediadata").addSnapshotListener { snapshot, _ ->
            totalMedia = snapshot?.size() ?: 0
        }
        db.collection("help_feedback").addSnapshotListener { snapshot, _ ->
            val docs = snapshot?.documents ?: emptyList()
            helpCount = docs.count { it.getString("type") == "Help" }
            feedbackCount = docs.count { it.getString("type") == "Feedback" }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard("Total Accounts", totalUsers.toString(), Icons.Default.People, Color(0xFF6200EE))
        StatCard("Online Now", onlineUsers.toString(), Icons.Default.Circle, Color(0xFF28A745))
        StatCard("Media Items", totalMedia.toString(), Icons.Default.PermMedia, Color(0xFFFFC107))
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Help & Feedback Overview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircleStat(label = "Help", count = helpCount, color = Color(0xFFFF5722))
            CircleStat(label = "Feedback", count = feedbackCount, color = Color(0xFF2196F3))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CircleStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(color.copy(alpha = 0.1f), CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontWeight = FontWeight.Medium)
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
    onFeedbacksClick: () -> Unit,
    onLyricsManagementClick: () -> Unit,
    onMediaLibraryClick: () -> Unit, 
    currentUserRole: UserRole?,
    userPermissions: List<String> = emptyList()
) {
    val isHost = currentUserRole == UserRole.HOST
    val isSubHost = currentUserRole == UserRole.SUB_HOST

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview - Visible to both
        AdminMenuCard(
            title = "Overview",
            icon = Icons.Default.Dashboard,
            description = "View app statistics and online users",
            onClick = onOverviewClick
        )

        // Home Slider - Only Host OR Sub-Host with permission
        if (isHost || (isSubHost && userPermissions.contains("Admin: Home Slider"))) {
            AdminMenuCard(
                title = "Home Slider",
                icon = Icons.Default.BurstMode,
                description = "Manage images on Home Screen slider",
                onClick = onHomeSliderClick
            )
        }

        // Manage Lyrics - Visible to Host/Sub-Host
        AdminMenuCard(
            title = "Manage Lyrics",
            icon = Icons.Default.Notes,
            description = "Add or edit Kirtan lyrics",
            onClick = onLyricsManagementClick
        )

        // Accounts - Only Host OR Sub-Host with permission
        if (isHost || (isSubHost && userPermissions.contains("Admin: Accounts"))) {
            AdminMenuCard(
                title = "Accounts",
                icon = Icons.Default.People,
                description = "Manage user roles and permissions",
                onClick = onAccountsClick
            )
        }
        
        // Password Requests - Visible to both (Host & Sub-Host)
        AdminMenuCard(
            title = "Password Requests",
            icon = Icons.Default.LockReset,
            description = "View and handle password reset requests",
            onClick = onPasswordRequestsClick
        )
        
        // Help & Feedback - Visible to both (Host & Sub-Host)
        AdminMenuCard(
            title = "Help & Feedback",
            icon = Icons.AutoMirrored.Filled.Help,
            description = "View user help requests and feedback",
            onClick = onFeedbacksClick
        )

        // Media Library - Only Host OR Sub-Host with permission
        if (isHost || (isSubHost && userPermissions.contains("Admin: Media Library"))) {
            AdminMenuCard(
                title = "Media Library",
                icon = Icons.Default.PermMedia,
                description = "Manage photos, videos, audio and docs",
                onClick = onMediaLibraryClick
            )
        }
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
