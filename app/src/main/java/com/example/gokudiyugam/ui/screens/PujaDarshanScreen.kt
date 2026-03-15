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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PujaDarshanScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit
) {
    var youtubeLink by remember { mutableStateOf(preferenceManager.getPujaDarshanLink() ?: "") }
    var hostAspectRatio by remember { mutableFloatStateOf(preferenceManager.getPujaAspectRatio()) }
    var isManualRatio by remember { mutableStateOf(preferenceManager.isPujaManualRatioEnabled()) }
    
    var isEditing by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val currentAspectRatio = if (isLandscape) 2.1f else if (isManualRatio) hostAspectRatio else 1.777f
    
    val linkUpdatedMsg = stringResource(R.string.link_updated)
    val currentUsername = preferenceManager.getCurrentUsername() ?: ""
    val canEdit = currentUserRole == UserRole.HOST || 
                 (currentUserRole == UserRole.SUB_HOST && preferenceManager.hasPermission(currentUsername, "screen_puja_darshan"))

    // Auto-delete logic: Clear Puja Darshan after 12:00 PM
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour >= 12) {
            // Clear preference link
            if (youtubeLink.isNotEmpty()) {
                preferenceManager.savePujaDarshanLink("")
                youtubeLink = ""
            }
            
            // Delete Puja Darshan posts from Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("mediadata")
                .whereEqualTo("type", "puja_darshan")
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                    }
                }
        }
    }

    BackHandler {
        if (isFullScreen) isFullScreen = false else onBack()
    }

    Scaffold(
        topBar = {
            if (!isFullScreen && !isLandscape) {
                TopAppBar(
                    title = { Text(stringResource(R.string.puja_darshan), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (canEdit) {
                            IconButton(onClick = { isEditing = !isEditing }) {
                                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Edit, contentDescription = null)
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
            if (isEditing && canEdit && !isLandscape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = youtubeLink,
                        onValueChange = { youtubeLink = it },
                        label = { Text("YouTube Link") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Height, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manual Aspect Ratio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Switch(checked = isManualRatio, onCheckedChange = { isManualRatio = it })
                    }
                    
                    if (isManualRatio) {
                        Slider(
                            value = hostAspectRatio,
                            onValueChange = { hostAspectRatio = it },
                            valueRange = 0.5f..3.0f,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text("Custom Ratio: ${"%.2f".format(hostAspectRatio)}", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            preferenceManager.savePujaDarshanLink(youtubeLink)
                            preferenceManager.savePujaAspectRatio(hostAspectRatio)
                            preferenceManager.savePujaManualRatioEnabled(isManualRatio)
                            isEditing = false
                            Toast.makeText(context, linkUpdatedMsg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save All Settings")
                    }
                }
            }

            if (youtubeLink.isNotEmpty()) {
                val processedUrl = remember(youtubeLink) { processYoutubeUrl(youtubeLink) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.aspectRatio(currentAspectRatio))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    PujaVideoWebView(url = processedUrl, onFullScreenToggle = { isFullScreen = it })
                }
                
                if (!isFullScreen && !isLandscape) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Jay Swaminarayan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Watching Puja Darshan Live", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_live_stream), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PujaVideoWebView(url: String, onFullScreenToggle: (Boolean) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // CSS to hide YouTube branding elements (title, logo, etc.)
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
                    // desktop user agent to avoid "Open App" banners
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

private fun processYoutubeUrl(url: String): String {
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
