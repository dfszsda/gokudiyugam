@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.model.MediaItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandirDarshanPhotoScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit,
    driveViewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val photos = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    val isUploading = driveViewModel.isUploading

    var selectedPhoto by remember { mutableStateOf<MediaItem?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var photoTitle by remember { mutableStateOf("") }
    var allowDownload by remember { mutableStateOf(true) }
    
    // Screen-specific permission logic
    var canEdit by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance("mediadata").collection("users").document(user.uid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val roleStr = doc.getString("role") ?: "NORMAL"
                    val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                    
                    // User is HOST or (User is SUB_HOST and has "Daily Darshan" permission)
                    canEdit = role == UserRole.HOST || (role == UserRole.SUB_HOST && permissions.contains("Daily Darshan"))
                }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                driveViewModel.selectedGoogleAccount = account?.account
                if (pendingUri != null) showAddDialog = true
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        driveViewModel.fetchCategoryItems("mandir_darshan")
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            driveViewModel.selectedGoogleAccount = lastAccount.account
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingUri = it
            if (driveViewModel.selectedGoogleAccount == null) {
                googleSignInLauncher.launch(DriveHelper.getGoogleSignInClient(context).signInIntent)
            } else {
                showAddDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mandir_darshan), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                FloatingActionButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Photo")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isFetching && photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No photos added yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                ) {
                    items(photos) { photoItem ->
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clickable { selectedPhoto = photoItem },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(photoItem.url),
                                contentDescription = photoItem.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
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

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Photo Details") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = photoTitle,
                            onValueChange = { photoTitle = it },
                            label = { Text("Photo Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Allow users to download?", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(checked = allowDownload, onCheckedChange = { allowDownload = it })
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (photoTitle.isNotBlank() && pendingUri != null) {
                            driveViewModel.uploadToCategory(
                                context = context,
                                uri = pendingUri!!,
                                title = photoTitle,
                                driveType = "photo",
                                category = "mandir_darshan",
                                canDownload = allowDownload
                            )
                            showAddDialog = false
                            photoTitle = ""
                            pendingUri = null
                        }
                    }) { Text("Upload") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (selectedPhoto != null) {
            AlertDialog(
                onDismissRequest = { selectedPhoto = null },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Show delete button only to authorized users
                        if (canEdit) {
                            IconButton(onClick = {
                                driveViewModel.deleteItem(context, selectedPhoto!!, "mandir_darshan")
                                selectedPhoto = null
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (selectedPhoto?.canDownload == true || currentUserRole == UserRole.HOST) {
                            TextButton(onClick = {
                                scope.launch {
                                    val urlToSave = selectedPhoto?.url
                                    if (urlToSave != null) {
                                        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
                                        val success = downloadAndSaveImage(context, urlToSave)
                                        if (success) showDownloadNotification(context)
                                        else Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                    selectedPhoto = null
                                }
                            }) {
                                Text("Download")
                            }
                        }
                        
                        TextButton(onClick = { selectedPhoto = null }) {
                            Text("Close")
                        }
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedPhoto?.url),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = selectedPhoto?.title ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            )
        }
    }
}

private suspend fun downloadAndSaveImage(context: Context, urlString: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlString)
        val connection = url.openConnection()
        connection.connect()
        val inputStream = connection.getInputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (bitmap == null) return@withContext false

        val filename = "Darshan_${System.currentTimeMillis()}.jpg"
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Gokudiyugam")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            val outputStream = contentResolver.openOutputStream(imageUri)
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(imageUri, contentValues, null, null)
                }
                return@withContext true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext false
}

private fun showDownloadNotification(context: Context) {
    val channelId = "download_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
        channelId,
        "Downloads",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    notificationManager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Download Complete")
        .setContentText("Photo has been saved to your gallery.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(1, notification)
}
