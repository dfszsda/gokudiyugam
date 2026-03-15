package com.example.gokudiyugam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    currentUserRole: UserRole? = null,
    onBack: () -> Unit,
    viewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val mediaItems = viewModel.currentCategoryItems
    val isFetching = viewModel.isFetching
    val isUploading = viewModel.isUploading

    var showPostDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchCategoryItems("all_uploads")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Media Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUserRole == UserRole.HOST || currentUserRole == UserRole.SUB_HOST) {
                ExtendedFloatingActionButton(
                    onClick = { showPostDialog = true },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    text = { Text("Upload to Cloud") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isFetching && mediaItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (mediaItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No files found in cloud storage")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mediaItems) { item ->
                            MediaItemCard(item)
                        }
                    }
                }
            }

            if (isUploading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Uploading...", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showPostDialog) {
            UploadDialog(
                onDismiss = { showPostDialog = false },
                onUpload = { uri, title, type ->
                    viewModel.uploadPublicPost(context, uri, title, type)
                    showPostDialog = false
                },
                onYouTubePost = { title, url ->
                    viewModel.postYouTubeLink(context, title, url, "all_uploads")
                    showPostDialog = false
                }
            )
        }
    }
}

@Composable
fun MediaItemCard(item: MediaItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Text(text = "Type: ${item.type}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
