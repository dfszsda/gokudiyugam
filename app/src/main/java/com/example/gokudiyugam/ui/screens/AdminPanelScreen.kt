@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    preferenceManager: PreferenceManager,
    onNavigateToMediaLibrary: () -> Unit,
    onBack: () -> Unit
) {
    var currentView by remember { mutableStateOf("main") }
    val currentUser = preferenceManager.getCurrentUsername() ?: ""
    val currentUserRole = preferenceManager.getUserRoleForAccount(currentUser)
    
    val isHost = currentUserRole == UserRole.HOST

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (currentView == "accounts") "User Accounts" else "Admin Panel", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentView == "accounts") currentView = "main" else onBack()
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
            if (currentView == "main") {
                AdminMainMenuView(
                    onAccountsClick = { currentView = "accounts" },
                    onMediaLibraryClick = onNavigateToMediaLibrary,
                    isHost = isHost
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Account storage in database is disabled as per request.\n\nOnly the Main Host can access administrative functions.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AdminMainMenuView(onAccountsClick: () -> Unit, onMediaLibraryClick: () -> Unit, isHost: Boolean) {
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
                description = "Database storage is currently disabled",
                onClick = onAccountsClick
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
