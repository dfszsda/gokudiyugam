package com.example.gokudiyugam.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.UserRole
import com.google.api.services.drive.model.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    currentUserRole: UserRole? = null,
    onBack: () -> Unit,
    viewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val driveFiles = viewModel.driveFiles
    val isFetching = viewModel.isFetching
    val isUploading = viewModel.isUploading
    val driveHelper = viewModel.driveHelper

    var showPostDialog by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(context, result)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Google Drive Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only Host/Sub-Host can post to the public library via Drive
            if (driveHelper != null && (currentUserRole == UserRole.HOST || currentUserRole == UserRole.SUB_HOST)) {
                ExtendedFloatingActionButton(
                    onClick = { showPostDialog = true },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    text = { Text("Post to Library") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (driveHelper == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = {
                            val signInClient = DriveHelper.getGoogleSignInClient(context)
                            signInLauncher.launch(signInClient.signInIntent)
                        }) {
                            Text("Sign in with Google for Drive Access")
                        }
                    }
                } else {
                    if (isFetching) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    
                    if (driveFiles.isEmpty() && !isFetching) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No files found in your App's Drive folder")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(driveFiles) { file ->
                                DriveFileCard(file)
                            }
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
                        Text("Uploading and sharing...", fontWeight = FontWeight.Bold)
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
                }
            )
        }
    }
}

@Composable
fun DriveFileCard(file: File) {
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
                Text(text = file.name ?: "Unnamed File", style = MaterialTheme.typography.titleMedium)
                Text(text = "MIME: ${file.mimeType ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
