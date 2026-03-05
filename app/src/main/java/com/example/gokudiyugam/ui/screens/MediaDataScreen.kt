@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gokudiyugam.model.MediaItem
import com.example.gokudiyugam.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDataScreen(
    currentUserRole: UserRole?,
    onBack: () -> Unit,
    viewModel: MediaViewModel = viewModel()
) {
    val context = LocalContext.current
    val mediaList = viewModel.mediaList
    var selectedType by remember { mutableStateOf("all") }
    val types = listOf("all", "photo", "video", "audio", "doc", "mandir_darshan", "sabha_timetable")

    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedItemForRepost by remember { mutableStateOf<MediaItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchMediaData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUserRole == UserRole.HOST || currentUserRole == UserRole.SUB_HOST) {
                FloatingActionButton(onClick = { showUploadDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Upload")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter Chips
            ScrollableTabRow(
                selectedTabIndex = types.indexOf(selectedType),
                edgePadding = 16.dp,
                divider = {}
            ) {
                types.forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        text = { Text(type.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            val filteredList = if (selectedType == "all") mediaList else mediaList.filter { it.type == selectedType }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { item ->
                    MediaCard(
                        item = item,
                        canManage = currentUserRole == UserRole.HOST || currentUserRole == UserRole.SUB_HOST,
                        onRepostClick = { selectedItemForRepost = item },
                        onDeleteClick = { itemToDelete = item }
                    )
                }
            }
        }

        if (showUploadDialog) {
            UploadDialog(
                onDismiss = { showUploadDialog = false },
                onUpload = { uri, title, type ->
                    viewModel.uploadFile(uri, title, type)
                    showUploadDialog = false
                }
            )
        }

        if (selectedItemForRepost != null) {
            RepostDialog(
                onDismiss = { selectedItemForRepost = null },
                onRepost = { category ->
                    selectedItemForRepost?.let { viewModel.repostItem(it, category) }
                    selectedItemForRepost = null
                }
            )
        }

        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Media") },
                text = { Text("Are you sure you want to delete '${itemToDelete?.title}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            itemToDelete?.let { viewModel.deleteMediaItem(it) }
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
                }
            )
        }
        
        if (viewModel.isUploading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun MediaCard(
    item: MediaItem, 
    canManage: Boolean, 
    onRepostClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (item.type == "photo" || item.type == "mandir_darshan") {
                AsyncImage(
                    model = item.url,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text(text = "Type: ${item.type}", style = MaterialTheme.typography.bodySmall)
                }
                
                Row {
                    if (canManage) {
                        IconButton(onClick = onRepostClick) {
                            Icon(Icons.Default.Send, contentDescription = "Post to screen")
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepostDialog(onDismiss: () -> Unit, onRepost: (String) -> Unit) {
    val categories = listOf("photo", "video", "audio", "doc", "mandir_darshan", "sabha_timetable")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post to Screen") },
        text = {
            Column {
                Text("Select where you want to show this item:")
                Spacer(modifier = Modifier.height(16.dp))
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = category }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCategory == category, onClick = { selectedCategory = category })
                        Text(text = category.replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onRepost(selectedCategory) }) {
                Text("Post Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UploadDialog(onDismiss: () -> Unit, onUpload: (Uri, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("photo") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload File") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                
                Text("Select Type:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("photo", "video", "audio", "doc").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }
                
                Button(onClick = { launcher.launch(when(selectedType) {
                    "photo" -> "image/*"
                    "video" -> "video/*"
                    "audio" -> "audio/*"
                    else -> "*/*"
                }) }) {
                    Text(if (selectedUri == null) "Choose File" else "File Selected")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedUri?.let { onUpload(it, title, selectedType) } },
                enabled = title.isNotEmpty() && selectedUri != null
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
