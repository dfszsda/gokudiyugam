@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.MediaItem
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.network.YouTubeApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruhariDarshanScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit,
    driveViewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val window = activity?.window
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val firestoreVideos = driveViewModel.currentCategoryItems
    var youtubeVideos by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var youtubeConfig by remember { mutableStateOf<Map<String, String>?>(null) }
    
    val guruhariVideos = remember(firestoreVideos, youtubeVideos) {
        (firestoreVideos + youtubeVideos).distinctBy { it.id }.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
    }
    
    var selectedVideo by remember { mutableStateOf<MediaItem?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    var youtubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    
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
                    
                    canEdit = role == UserRole.HOST || (role == UserRole.SUB_HOST && permissions.contains("Daily Darshan"))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance("mediadata")
            val configDoc = db.collection("config").document("api_keys").get().await()
            if (configDoc.exists()) {
                val apiKey = configDoc.getString("youtube_api_key") ?: ""
                val channelId = configDoc.getString("guruhari_channel_id") ?: ""
                val playlistId = configDoc.getString("guruhari_playlist_id") ?: ""
                youtubeConfig = mapOf("apiKey" to apiKey, "channelId" to channelId, "playlistId" to playlistId)
                
                if (apiKey.isNotBlank()) {
                    val channelVideos = if (channelId.isNotBlank()) YouTubeApiService.fetchLatestVideos(apiKey, channelId) else emptyList()
                    val playlistVideos = if (playlistId.isNotBlank()) YouTubeApiService.fetchPlaylistVideos(apiKey, playlistId) else emptyList()
                    youtubeVideos = (channelVideos + playlistVideos).distinctBy { it.id }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GuruhariDarshan", "Error fetching YouTube config/videos: ${e.message}")
        }
    }

    LaunchedEffect(Unit) {
        driveViewModel.fetchCategoryItems("guruhari_darshan")
    }

    LaunchedEffect(guruhariVideos) {
        if (selectedVideo == null && guruhariVideos.isNotEmpty()) {
            selectedVideo = guruhariVideos.first()
        }
    }

    // Effect to load new video when selection changes
    LaunchedEffect(selectedVideo) {
        selectedVideo?.let { video ->
            val videoId = extractYoutubeVideoId(video.url)
            if (videoId != null) {
                youtubePlayerRef?.loadVideo(videoId, 0f)
            }
        }
    }

    // Manage System Bars for Fullscreen/Landscape
    LaunchedEffect(isLandscape, isFullScreen) {
        window?.let {
            val controller = WindowInsetsControllerCompat(it, it.decorView)
            if (isLandscape || isFullScreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false 
        } else if (isLandscape) {
            onBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            if (!isFullScreen && !isLandscape) {
                TopAppBar(
                    title = { Text(stringResource(R.string.guruhari_darshan), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (canEdit) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Video")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(if (isFullScreen || isLandscape) PaddingValues(0.dp) else innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Video Player Section - Only shown when a video is selected
                if (selectedVideo != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isLandscape || isFullScreen) Modifier.fillMaxHeight() else Modifier.aspectRatio(1.777f))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                YouTubePlayerView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    lifecycleOwner.lifecycle.addObserver(this)
                                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                        override fun onReady(youTubePlayer: YouTubePlayer) {
                                            youtubePlayerRef = youTubePlayer
                                            val videoId = extractYoutubeVideoId(selectedVideo!!.url)
                                            if (videoId != null) {
                                                youTubePlayer.loadVideo(videoId, 0f)
                                            }
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Close button for player if not in fullscreen/landscape to go back to list view
                        if (!isFullScreen && !isLandscape) {
                            IconButton(
                                onClick = { selectedVideo = null },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Close Player", tint = Color.White)
                            }
                        }
                    }
                }

                if (!isFullScreen && !isLandscape) {
                    if (driveViewModel.isFetching && guruhariVideos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (guruhariVideos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text(text = "No Videos Available", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(guruhariVideos) { video ->
                                GuruhariVideoCard(
                                    video = video,
                                    isSelected = selectedVideo?.id == video.id,
                                    canDelete = canEdit && video.type != "youtube",
                                    onClick = { 
                                        selectedVideo = video 
                                    },
                                    onDelete = {
                                        FirebaseFirestore.getInstance("mediadata").collection("mediadata").document(video.id).delete()
                                        if (selectedVideo?.id == video.id) selectedVideo = null
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            // Explore Channel Button
            if (!isFullScreen && !isLandscape && youtubeConfig?.get("channelId")?.isNotBlank() == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = {
                            val channelId = youtubeConfig?.get("channelId")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/$channelId"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2F1)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Explore Channel", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var urls by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Guruhari Darshan Links") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Common Prefix") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = urls,
                        onValueChange = { urls = it },
                        label = { Text("YouTube Links (One per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("https://youtube.com/...\nhttps://youtu.be/...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urls.isNotEmpty()) {
                            val urlList = urls.split("\n").filter { it.trim().isNotEmpty() }
                            urlList.forEachIndexed { index, url ->
                                val finalTitle = if (urlList.size > 1) "$title ${index + 1}" else title
                                driveViewModel.postYouTubeLink(context, finalTitle, url.trim(), "guruhari_darshan")
                            }
                            showAddDialog = false
                        }
                    }
                ) { Text("Post All") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun GuruhariVideoCard(
    video: MediaItem,
    isSelected: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val videoId = extractYoutubeVideoId(video.url)
    val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.777f)) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play overlay icon
                Surface(
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
                Text(
                    text = "Guruhari Darshan",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun extractYoutubeVideoId(url: String): String? {
    return try {
        val patterns = listOf(
            "v=([^&]+)",
            "youtu.be/([^?]+)",
            "embed/([^?]+)",
            "live/([^?]+)",
            "shorts/([^?]+)"
        )
        for (p in patterns) {
            val matcher = java.util.regex.Pattern.compile(p).matcher(url)
            if (matcher.find()) return matcher.group(1)
        }
        null
    } catch (e: Exception) { null }
}
