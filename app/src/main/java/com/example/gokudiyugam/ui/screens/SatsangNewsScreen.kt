@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.MediaItem
import com.example.gokudiyugam.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatsangNewsScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onNavigateBack: () -> Unit,
    driveViewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val newsItems = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching

    // Screen-specific permission logic
    var canEdit by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        driveViewModel.fetchCategoryItems("news")
        
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance("mediadata").collection("users").document(user.uid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val roleStr = doc.getString("role") ?: "NORMAL"
                    val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                    
                    // User is HOST or (User is SUB_HOST and has "News" permission)
                    canEdit = role == UserRole.HOST || (role == UserRole.SUB_HOST && permissions.contains("News"))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.satsang_news), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(onClick = { 
                    Toast.makeText(context, "Use Media Library to post news updates", Toast.LENGTH_LONG).show()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add News")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isFetching && newsItems.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (newsItems.isEmpty()) {
                Text(
                    "No news available at the moment.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newsItems) { item ->
                        NewsCard(
                            item = item, 
                            canDelete = canEdit,
                            onDelete = {
                                driveViewModel.deleteItem(context, item, "news")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(item: MediaItem, canDelete: Boolean, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Newspaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Satsang Update", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}
