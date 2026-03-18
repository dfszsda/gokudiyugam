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

    val canEdit = preferenceManager.hasPermission(preferenceManager.getCurrentUsername() ?: "", "Sabha")

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
                                                val translations = AIService.translateToAll(sabhaTitleEn)
                                                sabhaTitleGu = translations["gu"] ?: ""
                                                sabhaTitleHi = translations["hi"] ?: ""
                                                isTranslating = false
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Translate, contentDescription = "Auto Translate")
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
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val finalTitle = if (sabhaTitleGu.isNotBlank()) sabhaTitleGu else sabhaTitleEn
                        if (finalTitle.isNotBlank()) {
                            driveViewModel.postYouTubeLink(context, finalTitle, "text_update", "sabha_timetable")
                            showAddDialog = false
                            sabhaTitleEn = ""; sabhaTitleGu = ""; sabhaTitleHi = ""
                        }
                    }) { Text("Post Update") }
                },
                dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun DynamicSabhaItem(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun NewSabhaItem(name: String, time: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFFFE0B2)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF424242))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
            }
        }
    }
}
