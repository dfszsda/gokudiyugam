@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.MediaItem
import com.example.gokudiyugam.model.Kirtan
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

enum class FestivalView {
    MAIN, PHOTOS, VIDEOS, AUDIOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivalsScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit,
    onNavigateToVideoPlayer: (String, String) -> Unit,
    onNavigateToAudioPlayer: (String) -> Unit,
    driveViewModel: DriveViewModel = viewModel(),
    kirtanViewModel: KirtanViewModel = viewModel()
) {
    var currentView by remember { mutableStateOf(FestivalView.MAIN) }
    val context = LocalContext.current
    
    var canEdit by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance("mediadata").collection("users").document(user.uid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val roleStr = doc.getString("role") ?: "NORMAL"
                    val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                    canEdit = role == UserRole.HOST || (role == UserRole.SUB_HOST && permissions.contains("Daily Darshan"))
                }
            }
        }
        kirtanViewModel.initController(context)
    }

    LaunchedEffect(currentView) {
        when(currentView) {
            FestivalView.PHOTOS -> driveViewModel.fetchCategoryItems("photo")
            FestivalView.VIDEOS -> driveViewModel.fetchCategoryItems("video")
            FestivalView.AUDIOS -> driveViewModel.fetchCategoryItems("audio")
            else -> {}
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
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = currentView != FestivalView.MAIN) {
        currentView = FestivalView.MAIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (currentView) {
                        FestivalView.MAIN -> stringResource(R.string.festivals)
                        FestivalView.PHOTOS -> "Photos"
                        FestivalView.VIDEOS -> "Videos"
                        FestivalView.AUDIOS -> "Audios"
                    }
                    Text(titleText, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentView == FestivalView.MAIN) onBack() else currentView = FestivalView.MAIN
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentView) {
                FestivalView.MAIN -> MainFestivalsSelection(onViewChange = { currentView = it })
                FestivalView.PHOTOS -> PhotosGalleryPage(driveViewModel, canEdit, googleSignInLauncher)
                FestivalView.VIDEOS -> VideosGalleryPage(driveViewModel, canEdit, googleSignInLauncher, onNavigateToVideoPlayer)
                FestivalView.AUDIOS -> AudiosGalleryPage(driveViewModel, kirtanViewModel, canEdit, googleSignInLauncher, onNavigateToAudioPlayer)
            }
        }
    }
}

@Composable
fun AudiosGalleryPage(
    driveViewModel: DriveViewModel,
    kirtanViewModel: KirtanViewModel,
    canEdit: Boolean,
    googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    onNavigateToAudioPlayer: (String) -> Unit
) {
    val context = LocalContext.current
    val audios = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(kirtanViewModel.errorMessage) {
        kirtanViewModel.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            kirtanViewModel.errorMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isFetching && audios.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = audios, key = { it.id }) { audioItem ->
                    val kirtan = Kirtan(id = audioItem.id, title = audioItem.title, category = "audio", fileUri = audioItem.url)
                    Surface(
                        onClick = { 
                            val playlist = audios.map { Kirtan(id = it.id, title = it.title, category = "audio", fileUri = it.url) }
                            kirtanViewModel.playKirtan(context, kirtan, playlist)
                            onNavigateToAudioPlayer("audio")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (kirtanViewModel.currentKirtan?.id == audioItem.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (kirtanViewModel.currentKirtan?.id == audioItem.id && kirtanViewModel.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Play/Pause", tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = audioItem.title, fontWeight = FontWeight.Bold)
                                audioItem.expiryTimestamp?.let {
                                    val days = ((it - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                                    Text("Expires in $days days", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                            if (canEdit) {
                                IconButton(onClick = { driveViewModel.deleteItem(context, audioItem, "audio") }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (canEdit && !isFetching) {
            ExtendedFloatingActionButton(
                onClick = { 
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account == null) googleSignInLauncher.launch(DriveHelper.getGoogleSignInClient(context).signInIntent)
                    else { driveViewModel.selectedGoogleAccount = account.account; showAddDialog = true }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = null) },
                text = { Text("Add Audio") }
            )
        }
    }

    if (showAddDialog) {
        UploadFestivalDialog(type = "audio", onDismiss = { showAddDialog = false }, driveViewModel = driveViewModel)
    }
}

@Composable
fun PhotosGalleryPage(
    driveViewModel: DriveViewModel, 
    canEdit: Boolean,
    googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    val context = LocalContext.current
    val photos = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    var selectedPhoto by remember { mutableStateOf<MediaItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isFetching && photos.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 128.dp), modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(items = photos, key = { it.id }) { photoItem ->
                    Image(
                        painter = rememberAsyncImagePainter(photoItem.url),
                        contentDescription = photoItem.title,
                        modifier = Modifier.padding(4.dp).aspectRatio(1f).clip(MaterialTheme.shapes.medium).clickable { selectedPhoto = photoItem },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        if (canEdit && !isFetching) {
            ExtendedFloatingActionButton(
                onClick = { 
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account == null) googleSignInLauncher.launch(DriveHelper.getGoogleSignInClient(context).signInIntent)
                    else { driveViewModel.selectedGoogleAccount = account.account; showAddDialog = true }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null) },
                text = { Text("Add Photo") }
            )
        }
    }

    if (showAddDialog) {
        UploadFestivalDialog(type = "photo", onDismiss = { showAddDialog = false }, driveViewModel = driveViewModel)
    }

    if (selectedPhoto != null) {
        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            confirmButton = { TextButton(onClick = { selectedPhoto = null }) { Text("Close") } },
            dismissButton = {
                if (canEdit) {
                    TextButton(onClick = { driveViewModel.deleteItem(context, selectedPhoto!!, "photo"); selectedPhoto = null }) {
                        Text("Delete", color = Color.Red)
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painter = rememberAsyncImagePainter(selectedPhoto?.url), contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), contentScale = ContentScale.Fit)
                    Text(text = selectedPhoto?.title ?: "", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun VideosGalleryPage(
    driveViewModel: DriveViewModel,
    canEdit: Boolean,
    googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    onNavigateToVideoPlayer: (String, String) -> Unit
) {
    val context = LocalContext.current
    val videos = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isFetching && videos.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 128.dp), modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(items = videos, key = { it.id }) { videoItem ->
                    Card(modifier = Modifier.padding(4.dp).aspectRatio(1f).clickable { onNavigateToVideoPlayer(videoItem.title, videoItem.url) }) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Icon(imageVector = Icons.Default.PlayCircle, contentDescription = "Play", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(videoItem.title, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                                if (canEdit) {
                                    IconButton(onClick = { driveViewModel.deleteItem(context, videoItem, "video") }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (canEdit && !isFetching) {
            ExtendedFloatingActionButton(
                onClick = { 
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account == null) googleSignInLauncher.launch(DriveHelper.getGoogleSignInClient(context).signInIntent)
                    else { driveViewModel.selectedGoogleAccount = account.account; showAddDialog = true }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(imageVector = Icons.Default.VideoCall, contentDescription = null) },
                text = { Text("Add Video") }
            )
        }
    }

    if (showAddDialog) {
        UploadFestivalDialog(type = "video", onDismiss = { showAddDialog = false }, driveViewModel = driveViewModel)
    }
}

@Composable
fun UploadFestivalDialog(type: String, onDismiss: () -> Unit, driveViewModel: DriveViewModel) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isAutoDelete by remember { mutableStateOf(false) }
    var durationDays by remember { mutableFloatStateOf(1f) }
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${type.replaceFirstChar { it.uppercase() }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { pickerLauncher.launch(when(type){"photo"->"image/*";"video"->"video/*";else->"audio/*"}) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedUri == null) "Select File" else "File Selected")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAutoDelete, onCheckedChange = { isAutoDelete = it })
                    Text("Auto Delete?")
                }
                if (isAutoDelete) {
                    Text("Days: ${durationDays.toInt()}")
                    Slider(value = durationDays, onValueChange = { durationDays = it }, valueRange = 1f..30f)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && selectedUri != null) {
                        val expiry = if (isAutoDelete) System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000) else null
                        driveViewModel.uploadFestivalItem(context, selectedUri!!, title, type, expiryTimestamp = expiry)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !driveViewModel.isUploading
            ) {
                if (driveViewModel.isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MainFestivalsSelection(onViewChange: (FestivalView) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FestivalCard(title = "Photos", icon = Icons.Default.PhotoLibrary, color = Color(0xFFE91E63)) { onViewChange(FestivalView.PHOTOS) }
        FestivalCard(title = "Videos", icon = Icons.Default.VideoLibrary, color = Color(0xFF2196F3)) { onViewChange(FestivalView.VIDEOS) }
        FestivalCard(title = "Audios", icon = Icons.Default.LibraryMusic, color = Color(0xFF4CAF50)) { onViewChange(FestivalView.AUDIOS) }
    }
}

@Composable
fun FestivalCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().background(color.copy(alpha = 0.1f)).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = color)
            Spacer(modifier = Modifier.width(24.dp))
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = color)
        }
    }
}
