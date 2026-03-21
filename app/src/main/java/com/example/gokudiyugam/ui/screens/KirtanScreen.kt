@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.UserRole
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KirtanScreen(
    onBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    kirtanViewModel: KirtanViewModel = viewModel(),
    driveViewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance("mediadata")
    val preferenceManager = remember { PreferenceManager(context) }
    
    // Screen-specific permission logic
    var canEdit by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf(UserRole.NORMAL) }
    
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            db.collection("users").document(user.uid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val roleStr = doc.getString("role") ?: "NORMAL"
                    userRole = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                    
                    // User is HOST or (User is SUB_HOST and has "Kirtan" permission)
                    canEdit = userRole == UserRole.HOST || (userRole == UserRole.SUB_HOST && permissions.contains("Kirtan"))
                }
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    var selectedCategoryForAdd by remember { mutableStateOf("Arati") }
    var kirtanTitle by remember { mutableStateOf("") }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioName by remember { mutableStateOf("No file selected") }

    val staticCategories = listOf(
        "Arati", "Thal", "Dhun", "Prathana", "Bhajan", "Puja Vidhi", "Others", "Favorite", "All Kirtans"
    )
    
    // Combined categories: Static + Dynamic (from Firestore)
    val allCategories = (staticCategories + kirtanViewModel.dynamicCategories).distinct()
    
    val filteredCategories = if (searchQuery.isEmpty()) {
        allCategories
    } else {
        allCategories.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAudioUri = it
            selectedAudioName = it.lastPathSegment ?: "Selected Audio"
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
                showAddDialog = true
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        kirtanViewModel.initController(context)
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            driveViewModel.selectedGoogleAccount = lastAccount.account
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kirtan_muktavali), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (userRole == UserRole.HOST) {
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.LibraryAdd, contentDescription = "Add Category")
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
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(onClick = {
                    if (driveViewModel.selectedGoogleAccount == null) {
                        googleSignInLauncher.launch(DriveHelper.getGoogleSignInClient(context).signInIntent)
                    } else {
                        showAddDialog = true
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Kirtan")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search Kirtan Category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredCategories) { category ->
                    KirtanCategoryCard(
                        label = category,
                        isFavorite = category == "Favorite",
                        isAll = category == "All Kirtans",
                        isDynamic = kirtanViewModel.dynamicCategories.contains(category),
                        canDelete = userRole == UserRole.HOST,
                        onDelete = { kirtanViewModel.removeCategory(category) },
                        onClick = { onNavigateToPlayer(category) }
                    )
                }
            }
        }

        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add New Category") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            kirtanViewModel.addCategory(newCategoryName)
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Kirtan") },
                text = {
                    Column {
                        Text("Category:", fontWeight = FontWeight.Bold)
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) { 
                                Text(selectedCategoryForAdd)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                allCategories.filter { it != "Favorite" && it != "All Kirtans" }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) }, 
                                        onClick = { 
                                            selectedCategoryForAdd = cat
                                            expanded = false 
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = kirtanTitle, 
                            onValueChange = { kirtanTitle = it }, 
                            label = { Text("Kirtan Title") }, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Select Audio File:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.AudioFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pick Audio")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedAudioName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (kirtanTitle.isNotBlank() && selectedAudioUri != null) {
                                driveViewModel.uploadToCategory(
                                    context = context,
                                    uri = selectedAudioUri!!,
                                    title = kirtanTitle,
                                    driveType = "audio",
                                    category = selectedCategoryForAdd.lowercase()
                                )
                                showAddDialog = false
                                kirtanTitle = ""
                                selectedAudioUri = null
                                selectedAudioName = "No file selected"
                            } else {
                                Toast.makeText(context, "Please enter title and select audio", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !driveViewModel.isUploading
                    ) {
                        if (driveViewModel.isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Upload")
                        }
                    }
                },
                dismissButton = { 
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } 
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun KirtanCategoryCard(
    label: String, 
    isFavorite: Boolean = false,
    isAll: Boolean = false,
    isDynamic: Boolean = false,
    canDelete: Boolean = false,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (isDynamic && canDelete) showDeleteConfirm = true }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFavorite -> Color(0xFFFFEBEE)
                isAll -> Color(0xFFE3F2FD)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = when {
                    isFavorite -> Icons.Default.Favorite
                    isAll -> Icons.Default.AllInclusive
                    else -> Icons.Default.MusicNote
                },
                contentDescription = null,
                tint = when {
                    isFavorite -> Color.Red
                    isAll -> Color(0xFF1976D2)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '$label'? (Existing kirtans won't be deleted but will be hidden from this list)") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
