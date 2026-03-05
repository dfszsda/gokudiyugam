package com.example.gokudiyugam.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruhariDarshanScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit
) {
    var youtubeLink by remember { mutableStateOf(preferenceManager.getGuruhariDarshanLink() ?: "") }
    var isEditing by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val linkUpdatedMsg = stringResource(R.string.link_updated)
    
    val currentUsername = preferenceManager.getCurrentUsername() ?: ""
    val canEdit = currentUserRole == UserRole.HOST || 
                 (currentUserRole == UserRole.SUB_HOST && preferenceManager.hasPermission(currentUsername, "screen_guruhari_darshan"))

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
                    title = { Text(stringResource(R.string.guruhari_darshan), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (canEdit) {
                            IconButton(onClick = { isEditing = !isEditing }) {
                                Icon(
                                    if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                                    contentDescription = if (isEditing) stringResource(R.string.save_link) else stringResource(R.string.edit_link)
                                )
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
                .padding(if (isFullScreen) PaddingValues(0.dp) else innerPadding)
        ) {
            if (isEditing && canEdit) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = youtubeLink,
                        onValueChange = { youtubeLink = it },
                        label = { Text(stringResource(R.string.youtube_live_link)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            preferenceManager.saveGuruhariDarshanLink(youtubeLink)
                            isEditing = false
                            Toast.makeText(context, linkUpdatedMsg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_link))
                    }
                }
            }

            if (youtubeLink.isNotEmpty()) {
                val processedUrl = remember(youtubeLink) { processGuruhariYoutubeUrl(youtubeLink) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    GuruhariVideoWebView(
                        url = processedUrl,
                        onFullScreenToggle = { isFullScreen = it }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_live_stream),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GuruhariVideoWebView(
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
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}

private fun processGuruhariYoutubeUrl(url: String): String {
    return try {
        if (url.contains("youtube.com/watch?v=")) {
            val videoId = url.split("v=")[1].split("&")[0]
            "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0"
        } else if (url.contains("youtu.be/")) {
            val videoId = url.split("youtu.be/")[1].split("?")[0]
            "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0"
        } else if (url.contains("youtube.com/live/")) {
            val videoId = url.split("live/")[1].split("?")[0]
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
fun GuruhariDarshanScreenPreview() {
    val context = LocalContext.current
    GokudiyugamTheme {
        GuruhariDarshanScreen(
            preferenceManager = PreferenceManager(context),
            currentUserRole = UserRole.HOST,
            onBack = {}
        )
    }
}
