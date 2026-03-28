@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

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
    
    val guruhariVideos = driveViewModel.currentCategoryItems
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen || isLandscape) PaddingValues(0.dp) else innerPadding)
        ) {
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
                                            youTubePlayer.cueVideo(videoId, 0f)
                                        }
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (!isFullScreen && !isLandscape) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(selectedVideo?.title ?: "Jay Swaminarayan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Watching Guruhari Darshan Live", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(guruhariVideos) { video ->
                            GuruhariVideoCard(
                                video = video,
                                isSelected = selectedVideo?.id == video.id,
                                canDelete = canEdit,
                                onClick = { selectedVideo = video },
                                onDelete = {
                                    FirebaseFirestore.getInstance("mediadata").collection("mediadata").document(video.id).delete()
                                    if (selectedVideo?.id == video.id) selectedVideo = null
                                }
                            )
                        }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = video.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Guruhari Darshan Video", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
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
