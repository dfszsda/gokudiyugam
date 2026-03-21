@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val types = listOf("all", "photo", "video", "audio", "doc", "youtube", "mandir_darshan", "puja_darshan", "guruhari_darshan", "sabha_timetable")

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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column {
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
                            text = { Text(type.replaceFirstChar { it.uppercase() }.replace("_", " ")) }
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

            // Modern Upload Progress Overlay
            AnimatedVisibility(
                visible = viewModel.isUploading,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (viewModel.uploadProgress >= 1f) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = viewModel.uploadStatus, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LinearProgressIndicator(
                            progress = { viewModel.uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        if (showUploadDialog) {
            UploadDialog(
                onDismiss = { showUploadDialog = false },
                onUpload = { uri, title, type ->
                    viewModel.uploadFile(uri, title, type)
                    showUploadDialog = false
                },
                onYouTubePost = { title, url ->
                    viewModel.postYouTubeLink(title, url)
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
    }
}

@Composable
fun MediaCard(
    item: MediaItem, 
    canManage: Boolean, 
    onRepostClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (item.type == "youtube" || item.url.contains("youtube.com") || item.url.contains("youtu.be")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                context.startActivity(intent)
            }
        },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (item.type == "photo" || item.type == "mandir_darshan") {
                AsyncImage(
                    model = item.url,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            } else if (item.type == "youtube" || item.url.contains("youtube.com") || item.url.contains("youtu.be")) {
                val videoId = try {
                    if (item.url.contains("v=")) item.url.substringAfter("v=").substringBefore("&")
                    else item.url.substringAfterLast("/")
                } catch (e: Exception) { "" }
                
                if (videoId.isNotEmpty()) {
                    val thumbUrl = "https://img.youtube.com/vi/$videoId/0.jpg"
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
                Text(
                    text = "Video Content",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text(text = "Type: ${item.type.replace("_", " ")}", style = MaterialTheme.typography.bodySmall)
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
    val categories = listOf("photo", "video", "audio", "doc", "youtube", "mandir_darshan", "puja_darshan", "guruhari_darshan", "sabha_timetable")
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
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCategory == category, onClick = { selectedCategory = category })
                        Text(text = category.replaceFirstChar { it.uppercase() }.replace("_", " "), modifier = Modifier.padding(start = 8.dp))
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
fun UploadDialog(
    onDismiss: () -> Unit, 
    onUpload: (Uri, String, String) -> Unit,
    onYouTubePost: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("photo") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var youtubeUrl by remember { mutableStateOf("") }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, 
                    onValueChange = { title = it }, 
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Select Type:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val types = listOf("photo", "video", "audio", "doc", "youtube")
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                
                if (selectedType == "youtube") {
                    OutlinedTextField(
                        value = youtubeUrl, 
                        onValueChange = { youtubeUrl = it }, 
                        label = { Text("YouTube Link") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Button(
                        onClick = { launcher.launch(when(selectedType) {
                            "photo" -> "image/*"
                            "video" -> "video/*"
                            "audio" -> "audio/*"
                            else -> "*/*"
                        }) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedUri == null) "Choose File" else "File Selected")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (selectedType == "youtube") {
                        onYouTubePost(title, youtubeUrl)
                    } else {
                        selectedUri?.let { onUpload(it, title, selectedType) }
                    }
                },
                enabled = title.isNotEmpty() && (selectedUri != null || (selectedType == "youtube" && youtubeUrl.isNotEmpty()))
            ) {
                Text(if (selectedType == "youtube") "Post Link" else "Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
