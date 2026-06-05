package com.example.gokudiyugam.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiManagementScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance("mediadata")
    
    var apiKey by remember { mutableStateOf("") }
    var pujaChannelId by remember { mutableStateOf("") }
    var guruhariChannelId by remember { mutableStateOf("") }
    var guruhariPlaylistId by remember { mutableStateOf("") }
    
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isPujaVisible by remember { mutableStateOf(false) }
    var isGuruhariVisible by remember { mutableStateOf(false) }
    var isPlaylistVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val doc = db.collection("config").document("api_keys").get().await()
            if (doc.exists()) {
                apiKey = doc.getString("youtube_api_key") ?: ""
                pujaChannelId = doc.getString("puja_channel_id") ?: ""
                guruhariChannelId = doc.getString("guruhari_channel_id") ?: ""
                guruhariPlaylistId = doc.getString("guruhari_playlist_id") ?: ""
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading config: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    fun saveConfig() {
        val config = mapOf(
            "youtube_api_key" to apiKey,
            "puja_channel_id" to pujaChannelId,
            "guruhari_channel_id" to guruhariChannelId,
            "guruhari_playlist_id" to guruhariPlaylistId,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("config").document("api_keys").set(config)
            .addOnSuccessListener {
                Toast.makeText(context, "Configuration saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Main Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            ApiConfigItem(
                label = "YouTube Data API Key",
                value = apiKey,
                onValueChange = { apiKey = it },
                isVisible = isApiKeyVisible,
                onVisibilityToggle = { isApiKeyVisible = !isApiKeyVisible }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Puja Darshan Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            ApiConfigItem(
                label = "Puja Darshan Channel ID",
                value = pujaChannelId,
                onValueChange = { pujaChannelId = it },
                isVisible = isPujaVisible,
                onVisibilityToggle = { isPujaVisible = !isPujaVisible }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Guruhari Darshan Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            ApiConfigItem(
                label = "Guruhari Darshan Channel ID",
                value = guruhariChannelId,
                onValueChange = { guruhariChannelId = it },
                isVisible = isGuruhariVisible,
                onVisibilityToggle = { isGuruhariVisible = !isGuruhariVisible }
            )
            
            ApiConfigItem(
                label = "Guruhari Darshan Playlist ID",
                value = guruhariPlaylistId,
                onValueChange = { guruhariPlaylistId = it },
                isVisible = isPlaylistVisible,
                onVisibilityToggle = { isPlaylistVisible = !isPlaylistVisible }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { saveConfig() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Configuration")
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    IconButton(onClick = { 
                        onValueChange(tempValue)
                        isEditing = false 
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF4CAF50))
                    }
                } else {
                    Text(
                        text = if (isVisible) value else "•".repeat(if (value.length > 20) 20 else value.length).ifEmpty { "Not Set" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                    IconButton(onClick = { 
                        tempValue = value
                        isEditing = true 
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFF9800))
                    }
                    IconButton(onClick = onVisibilityToggle) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility"
                        )
                    }
                }
                IconButton(onClick = { /* Info dialog */ }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFFF9800))
                }
            }
        }
    }
}
