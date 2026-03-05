package com.example.gokudiyugam.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    var isFullScreen by remember { mutableStateOf(false) }
    val processedUrl = remember(url) { processVideoUrl(url) }
    
    // AI feature state: Focus Mode (dims UI, maximizes focus on video)
    var aiFocusMode by remember { mutableStateOf(false) }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (aiFocusMode) {
                                Text(
                                    "AI Focus Mode Active", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { aiFocusMode = !aiFocusMode }) {
                            Icon(
                                Icons.Default.AutoAwesome, 
                                contentDescription = "AI Focus Mode",
                                tint = if (aiFocusMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (aiFocusMode) Color.Black else MaterialTheme.colorScheme.surface,
                        titleContentColor = if (aiFocusMode) Color.White else MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = if (aiFocusMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        containerColor = if (aiFocusMode) Color.Black else MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else padding)
                .background(if (aiFocusMode) Color.Black else Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                VideoWebView(
                    url = processedUrl,
                    onFullScreenToggle = { fullScreen ->
                        isFullScreen = fullScreen
                    }
                )
            }
            
            if (!isFullScreen && !aiFocusMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Insight", 
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This video is identified as a spiritual event. Use Focus Mode (top right) to minimize distractions during your viewing experience.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoWebView(
    url: String,
    onFullScreenToggle: (Boolean) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
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
                }
                
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { _ ->
            // No update logic needed
        }
    )
}

private fun processVideoUrl(url: String): String {
    return try {
        if (url.contains("youtube.com/watch?v=")) {
            val videoId = url.split("v=")[1].split("&")[0]
            "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0"
        } else if (url.contains("youtu.be/")) {
            val videoId = url.split("youtu.be/")[1].split("?")[0]
            "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0"
        } else {
            url
        }
    } catch (e: Exception) {
        url
    }
}

@Preview(showBackground = true)
@Composable
fun VideoPlayerPreview() {
    GokudiyugamTheme {
        VideoPlayerScreen(
            title = "Sample Spiritual Video",
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            onBack = {}
        )
    }
}
