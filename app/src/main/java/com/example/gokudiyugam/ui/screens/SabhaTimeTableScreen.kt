@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.ai.AIService
import com.example.gokudiyugam.drive.DriveHelper
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.UserRole
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabhaTimeTableScreen(
    onBack: () -> Unit, 
    onSabhaClick: (String) -> Unit,
    preferenceManager: PreferenceManager = PreferenceManager(LocalContext.current),
    currentUserRole: UserRole? = null,
    driveViewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dynamicSabhas = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    
    var showAddDialog by remember { mutableStateOf(false) }
    var sabhaTitleEn by remember { mutableStateOf("") }
    var sabhaTitleGu by remember { mutableStateOf("") }
    var sabhaTitleHi by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    val translationMethod = remember { preferenceManager.getTranslationMethod() }

    // Screen-specific permission logic
    val (canEdit, setCanEdit) = remember { mutableStateOf(false) }
    
    LaunchedEffect(currentUserRole) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance("mediadata").collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                val permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val roleStr = doc.getString("role") ?: "NORMAL"
                val role = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.NORMAL }
                
                // User is HOST or (User is SUB_HOST and has "Sabha" permission)
                setCanEdit(role == UserRole.HOST || (role == UserRole.SUB_HOST && permissions.contains("Sabha")))
            }
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
        driveViewModel.fetchCategoryItems("sabha_timetable")
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            driveViewModel.selectedGoogleAccount = lastAccount.account
        }
    }

    val sabhas = listOf(
        stringResource(R.string.sabha_sanyukt) to stringResource(R.string.sabha_sanyukt_time),
        stringResource(R.string.sabha_yuva) to stringResource(R.string.sabha_yuva_time),
        stringResource(R.string.sabha_yuvati) to stringResource(R.string.sabha_yuvati_time),
        stringResource(R.string.sabha_bal) to stringResource(R.string.sabha_bal_time),
        stringResource(R.string.sabha_balika) to stringResource(R.string.sabha_balika_time),
        stringResource(R.string.sabha_shishu) to stringResource(R.string.sabha_shishu_time),
        stringResource(R.string.sabha_yst) to stringResource(R.string.sabha_yst_time),
        stringResource(R.string.sabha_bss) to stringResource(R.string.sabha_bss_time),
        stringResource(R.string.sabha_mahila) to stringResource(R.string.sabha_mahila_time)
    )

    val primaryColor = Color(0xFFFF9800)
    val secondaryColor = Color(0xFFFFCC80)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sabha_schedule), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = {
                        if (driveViewModel.selectedGoogleAccount == null) {
                            googleSignInLauncher.launch(DriveHelper.getGoogleSignInClient(context).signInIntent)
                        } else {
                            showAddDialog = true
                        }
                    },
                    containerColor = primaryColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Schedule")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(primaryColor, secondaryColor)))
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(text = "Weekly Schedule", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(text = "Stay updated with your local Sabha timings.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }

            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)).background(Color.White)) {
                if (isFetching && dynamicSabhas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (dynamicSabhas.isNotEmpty()) {
                            item { Text("Latest Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }
                            items(dynamicSabhas) { item ->
                                DynamicSabhaItem(title = item.title, onClick = { /* View item if needed */ })
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        }

                        item { Text("Regular Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }
                        items(sabhas) { (name, time) ->
                            NewSabhaItem(name = name, time = time, onClick = { onSabhaClick(name) })
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Special Update") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = sabhaTitleEn,
                            onValueChange = { sabhaTitleEn = it },
                            label = { Text("Update (English/Hinglish)") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (isTranslating) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    IconButton(onClick = {
                                        if (sabhaTitleEn.isNotBlank()) {
                                            isTranslating = true
                                            scope.launch {
                                                val translations = AIService.getTranslatedText(sabhaTitleEn, preferenceManager)
                                                sabhaTitleGu = translations["gu"] ?: ""
                                                sabhaTitleHi = translations["hi"] ?: ""
                                                isTranslating = false
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (translationMethod == "smart") Icons.Default.Translate else Icons.Default.Spellcheck,
                                            contentDescription = "Auto Translate"
                                        )
                                    }
                                }
                            }
                        )
                        OutlinedTextField(
                            value = sabhaTitleGu,
                            onValueChange = { sabhaTitleGu = it },
                            label = { Text("ગુજરાતી (Gujarati)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sabhaTitleHi,
                            onValueChange = { sabhaTitleHi = it },
                            label = { Text("हिन्दी (Hindi)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Action Button with dynamic label
                        Button(
                            onClick = {
                                if (sabhaTitleEn.isNotBlank()) {
                                    isTranslating = true
                                    scope.launch {
                                        val translations = AIService.getTranslatedText(sabhaTitleEn, preferenceManager)
                                        sabhaTitleGu = translations["gu"] ?: ""
                                        sabhaTitleHi = translations["hi"] ?: ""
                                        isTranslating = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            enabled = !isTranslating
                        ) {
                            Icon(
                                imageVector = if (translationMethod == "smart") Icons.Default.Translate else Icons.Default.Spellcheck,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (translationMethod == "smart") "Smart Translate" else "Lipi Transliterate",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val lang = preferenceManager.getLanguage()
                        val finalTitle = when(lang) {
                            "gu" -> if (sabhaTitleGu.isNotBlank()) sabhaTitleGu else sabhaTitleEn
                            "hi" -> if (sabhaTitleHi.isNotBlank()) sabhaTitleHi else sabhaTitleEn
                            else -> sabhaTitleEn
                        }
                        if (finalTitle.isNotBlank()) {
                            driveViewModel.postYouTubeLink(context, finalTitle, "text_update", "sabha_timetable")
                            showAddDialog = false
                            sabhaTitleEn = ""; sabhaTitleGu = ""; sabhaTitleHi = ""
                        }
                    }) { Text("Post Update") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun NewSabhaItem(name: String, time: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFFF9800))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DynamicSabhaItem(title: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFE65100))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
