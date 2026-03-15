package com.example.gokudiyugam.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.MediaItem
import com.example.gokudiyugam.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruhariDarshanScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit,
    driveViewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val guruhariVideos = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    
    var selectedVideo by remember { mutableStateOf<MediaItem?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    val currentUsername = preferenceManager.getCurrentUsername() ?: ""
    val canEdit = currentUserRole == UserRole.HOST || 
                 (currentUserRole == UserRole.SUB_HOST && preferenceManager.hasPermission(currentUsername, "screen_guruhari_darshan"))

    LaunchedEffect(Unit) {
        driveViewModel.fetchCategoryItems("guruhari_darshan")
    }

    // Auto-select first video if none selected
    LaunchedEffect(guruhariVideos) {
        if (selectedVideo == null && guruhariVideos.isNotEmpty()) {
            selectedVideo = guruhariVideos.first()
        }
    }

    BackHandler {
        if (isFullScreen) isFullScreen = false else onBack()
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
                val processedUrl = remember(selectedVideo) { processGuruhariYoutubeUrl(selectedVideo!!.url) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.aspectRatio(1.777f))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    GuruhariVideoWebView(url = processedUrl, onFullScreenToggle = { isFullScreen = it })
                }
                
                if (!isFullScreen && !isLandscape) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(selectedVideo?.title ?: "Jay Swaminarayan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Watching Guruhari Darshan Live", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            if (!isFullScreen && !isLandscape) {
                if (isFetching && guruhariVideos.isEmpty()) {
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
                                    FirebaseFirestore.getInstance().collection("mediadata").document(video.id).delete()
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
        var url by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Guruhari Darshan Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("YouTube Link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotEmpty() && url.isNotEmpty()) {
                            driveViewModel.postYouTubeLink(context, title, url, "guruhari_darshan")
                            showAddDialog = false
                        }
                    }
                ) { Text("Post") }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GuruhariVideoWebView(url: String, onFullScreenToggle: (Boolean) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val css = ".ytp-chrome-top, .ytp-show-cards-title, .ytp-watermark, .ytp-youtube-button, .ytp-pause-overlay { display: none !important; }"
                        val js = "var style = document.createElement('style'); style.innerHTML = '$css'; document.head.appendChild(style);"
                        view?.evaluateJavascript(js, null)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                        super.onShowCustomView(view, callback)
                        onFullScreenToggle(true)
                    }
                    override fun onHideCustomView() {
                        super.onHideCustomView()
                        onFullScreenToggle(false)
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { webView ->
            if (webView.url != url && url.isNotEmpty()) {
                webView.loadUrl(url)
            }
        }
    )
}

private fun processGuruhariYoutubeUrl(url: String): String {
    val videoId = extractYoutubeVideoId(url)
    return if (videoId != null) {
        "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&controls=1&showinfo=0&iv_load_policy=3&cc_load_policy=0"
    } else {
        url
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
