@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.UserRole
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabhaSaarScreen(
    onBack: () -> Unit,
    driveViewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance("mediadata")
    
    val docItems = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    val isUploading = driveViewModel.isUploading

    var canEdit by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var docTitle by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("No file selected") }

    // Viewer State
    var selectedDocUrl by remember { mutableStateOf<String?>(null) }
    var selectedDocTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            db.collection("users").document(user.uid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val roleStr = doc.getString("role") ?: "NORMAL"
                    val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                    
                    canEdit = role == UserRole.HOST || (role == UserRole.SUB_HOST && permissions.contains("Sabha Saar"))
                }
            }
        }
        driveViewModel.fetchCategoryItems("sabha_saar")
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            selectedFileName = it.lastPathSegment ?: "Selected Document"
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                driveViewModel.selectedGoogleAccount = account?.account
                showAddDialog = true
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Toast.makeText(context, "Google Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = selectedDocUrl != null) {
        selectedDocUrl = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedDocUrl == null) "Sabha Saar" else selectedDocTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedDocUrl != null) selectedDocUrl = null else onBack()
                    }) {
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
            if (canEdit && selectedDocUrl == null) {
                FloatingActionButton(onClick = {
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account == null) {
                        googleSignInLauncher.launch(DriveHelper.getGoogleSignInClient(context).signInIntent)
                    } else {
                        driveViewModel.selectedGoogleAccount = account.account
                        showAddDialog = true
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Document")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (selectedDocUrl != null) {
                if (selectedDocUrl!!.lowercase().contains(".txt")) {
                    TextViewer(url = selectedDocUrl!!)
                } else {
                    DocumentViewer(url = selectedDocUrl!!)
                }
            } else {
                if (isFetching && docItems.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (docItems.isEmpty()) {
                    Text("No documents available.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(docItems) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDocTitle = item.title
                                        selectedDocUrl = item.url
                                    },
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            item.url.lowercase().contains(".pdf") -> Icons.Default.PictureAsPdf
                                            item.url.lowercase().contains(".doc") -> Icons.Default.Description
                                            item.url.lowercase().contains(".txt") -> Icons.Default.Article
                                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        val typeLabel = when {
                                            item.url.lowercase().contains(".pdf") -> "PDF Document"
                                            item.url.lowercase().contains(".doc") -> "Word Document"
                                            item.url.lowercase().contains(".txt") -> "Text File"
                                            else -> "Document"
                                        }
                                        Text(text = typeLabel, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    if (canEdit) {
                                        IconButton(onClick = { driveViewModel.deleteItem(context, item, "sabha_saar") }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isUploading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Upload Sabha Saar Doc") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = docTitle,
                            onValueChange = { docTitle = it },
                            label = { Text("Document Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { docPickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.UploadFile, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedUri == null) "Pick File (PDF/Word/Text)" else "File Selected")
                        }
                        if (selectedUri != null) {
                            Text(selectedFileName, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (docTitle.isNotBlank() && selectedUri != null) {
                                driveViewModel.uploadFestivalItem(
                                    context = context,
                                    uri = selectedUri!!,
                                    title = docTitle,
                                    category = "sabha_saar",
                                    folderId = "1D-GKrMOs-tT_4hFKEmhJS6nCNC-fvjAb"
                                )
                                showAddDialog = false
                                docTitle = ""
                                selectedUri = null
                            }
                        }
                    ) { Text("Upload") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DocumentViewer(url: String) {
    // PDF/DOCX using Google Docs Viewer
    val viewerUrl = "https://docs.google.com/viewer?url=$url&embedded=true"
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                loadUrl(viewerUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TextViewer(url: String) {
    var textContent by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                textContent = URL(url).readText()
            } catch (e: Exception) {
                textContent = "Error loading text: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Text(
                    text = textContent,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )
            }
        }
    }
}
