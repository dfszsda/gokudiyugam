package com.example.gokudiyugam.ui.screens

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.gokudiyugam.PreferenceManager
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

@SuppressLint("SourceLockedOrientationActivity")
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val activity = context as ComponentActivity
    val window = activity.window
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Use YouTube player if preference is YouTube Player or if it's a YouTube URL
    val playerPref = preferenceManager.getDefaultPlayer()
    val isYoutubeUrl = url.contains("youtube.com", ignoreCase = true) || 
                       url.contains("youtu.be", ignoreCase = true)
    
    val isDriveUrl = url.contains("drive.google.com")
    
    // Always use YouTube player if preference is set or it's a YouTube link
    val useYoutubePlayer = playerPref == "YouTube Player" || isYoutubeUrl

    // Convert Google Drive link to direct link for ExoPlayer
    val finalUrl = remember(url) { convertDriveUrlToDirectLink(url) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(finalUrl)
            setMediaItem(mediaItem)
            
            // Apply saved quality preference on startup
            val preferredQuality = preferenceManager.getPreferredVideoQuality()
            if (preferredQuality != "Auto") {
                val height = preferredQuality.replace("p", "").toIntOrNull() ?: Int.MAX_VALUE
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(Int.MAX_VALUE, height)
                    .build()
            }
            
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var youtubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    var youtubeCurrentSecond by remember { mutableFloatStateOf(0f) }
    var isFullScreen by remember { mutableStateOf(true) }
    
    // Quality States
    var showQualityDialog by remember { mutableStateOf(false) }
    var availableQualities by remember { mutableStateOf(listOf("Auto")) }
    var selectedQuality by remember { mutableStateOf(preferenceManager.getPreferredVideoQuality()) }

    // ExoPlayer Track Listener
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val qualities = mutableListOf("Auto")
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            if (format.height != androidx.media3.common.Format.NO_VALUE) {
                                qualities.add("${format.height}p")
                            }
                        }
                    }
                }
                availableQualities = qualities.distinct().sortedByDescending { 
                    it.replace("p", "").toIntOrNull() ?: 0 
                }
            }
        }
        exoPlayer.addListener(listener)
    }

    // Fullscreen UI Control
    LaunchedEffect(isFullScreen) {
        window.let {
            val controller = WindowInsetsControllerCompat(it, it.decorView)
            if (isFullScreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Cleanup orientation on leave
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    BackHandler { 
        if (!isFullScreen) {
            onBack()
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullScreen = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (useYoutubePlayer) {
            AndroidView(
                factory = { ctx ->
                    YouTubePlayerView(ctx).apply {
                        lifecycleOwner.lifecycle.addObserver(this)
                        
                        addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youtubePlayerRef = youTubePlayer
                                val videoId = extractYoutubeVideoId(url) ?: ""
                                youTubePlayer.loadVideo(videoId, 0f)
                                availableQualities = listOf("Auto", "1080p", "720p", "480p", "360p", "240p", "144p")
                            }
                            
                            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                                isPlaying = state == PlayerConstants.PlayerState.PLAYING
                            }
                            
                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                youtubeCurrentSecond = second
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Touch Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        )

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
                
                // Back Button
                IconButton(
                    onClick = {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        onBack()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Settings/Quality Button
                IconButton(
                    onClick = { showQualityDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Quality Settings", tint = Color.White)
                }

                // Orientation Toggle
                IconButton(
                    onClick = {
                        val currentOrientation = activity.requestedOrientation
                        if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = "Toggle Orientation", tint = Color.White)
                }

                // Title
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    maxLines = 1
                )

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Backward 5s
                    IconButton(
                        onClick = {
                            if (useYoutubePlayer) {
                                youtubePlayerRef?.seekTo(youtubeCurrentSecond - 5f)
                            } else {
                                val newPos = (exoPlayer.currentPosition - 5000).coerceAtLeast(0)
                                exoPlayer.seekTo(newPos)
                            }
                        },
                        modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay5, contentDescription = "Back 5s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Play/Pause
                    IconButton(
                        onClick = {
                            if (useYoutubePlayer) {
                                if (isPlaying) youtubePlayerRef?.pause() else youtubePlayerRef?.play()
                            } else {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                isPlaying = exoPlayer.isPlaying
                            }
                        },
                        modifier = Modifier.size(80.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            if (useYoutubePlayer) {
                                youtubePlayerRef?.seekTo(youtubeCurrentSecond + 10f)
                            } else {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                            }
                        },
                        modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                
                // Drive Warning
                if (isDriveUrl) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(Color.Red.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download Disabled", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        Dialog(onDismissRequest = { showQualityDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = "Select Video Quality",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn {
                        items(availableQualities) { quality ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (quality == selectedQuality),
                                        onClick = {
                                            selectedQuality = quality
                                            preferenceManager.savePreferredVideoQuality(quality)
                                            if (useYoutubePlayer) {
                                                // Note: Mobile YouTube iframe API has limited support for manual quality selection
                                            } else {
                                                val builder = exoPlayer.trackSelectionParameters.buildUpon()
                                                if (quality == "Auto") {
                                                    builder.clearVideoSizeConstraints()
                                                } else {
                                                    val height = quality.replace("p", "").toIntOrNull() ?: Int.MAX_VALUE
                                                    builder.setMaxVideoSize(Int.MAX_VALUE, height)
                                                }
                                                exoPlayer.trackSelectionParameters = builder.build()
                                            }
                                            showQualityDialog = false
                                        }
                                    )
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (quality == selectedQuality),
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = quality, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun convertDriveUrlToDirectLink(url: String): String {
    return when {
        url.contains("drive.google.com") -> {
            val fileId = extractDriveFileId(url)
            if (fileId != null) "https://drive.google.com/uc?id=$fileId&export=download" else url
        }
        else -> url
    }
}

private fun extractDriveFileId(url: String): String? {
    val patterns = listOf("/d/([^/]+)", "id=([^&]+)")
    for (p in patterns) {
        val matcher = java.util.regex.Pattern.compile(p).matcher(url)
        if (matcher.find()) return matcher.group(1)
    }
    return null
}

private fun extractYoutubeVideoId(url: String): String? {
    val patterns = listOf(
        "v=([^&]+)", 
        "youtu.be/([^?]+)", 
        "embed/([^?]+)", 
        "shorts/([^?]+)",
        "watch\\?v=([^&]+)"
    )
    for (p in patterns) {
        val matcher = java.util.regex.Pattern.compile(p).matcher(url)
        if (matcher.find()) return matcher.group(1)
    }
    return null
}
