package com.example.gokudiyugam.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    onBack: () -> Unit,
    onRestartApp: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(preferenceManager.getLanguage()) }
    var isDarkMode by remember { mutableStateOf(preferenceManager.isDarkMode()) }
    var selectedColor by remember { mutableIntStateOf(preferenceManager.getBackgroundColor()) }
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    // State for Profile Fields
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var profilePhotoUrl by remember { mutableStateOf("") }
    val isUpdating by remember { mutableStateOf(false) }

    // Fetch user profile from Firestore if it exists (Read-only check)
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            db.collection("profile").document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    firstName = user?.firstName ?: ""
                    middleName = user?.middleName ?: ""
                    lastName = user?.lastName ?: ""
                    mobileNumber = user?.mobileNumber ?: ""
                    dob = user?.dob ?: ""
                    profilePhotoUrl = user?.profilePhotoUrl ?: ""
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profilePhotoUrl = it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Section
            Text("Profile Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePhotoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profilePhotoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    TextButton(onClick = { launcher.launch("image/*") }) {
                        Text("Change Photo")
                    }

                    ProfileTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name", icon = Icons.Default.Person)
                    ProfileTextField(value = middleName, onValueChange = { middleName = it }, label = "Middle Name", icon = Icons.Default.Person)
                    ProfileTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", icon = Icons.Default.Person)
                    ProfileTextField(value = mobileNumber, onValueChange = { mobileNumber = it }, label = "Mobile Number", icon = Icons.Default.Phone)
                    ProfileTextField(value = dob, onValueChange = { dob = it }, label = "Date of Birth (DD/MM/YYYY)", icon = Icons.Default.Cake)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            Toast.makeText(context, "Database storage is disabled by Admin.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Profile (Disabled)")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // App Settings
            Text("App Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Language",
                subtitle = if (currentLanguage == "gu") "Gujarati" else "English",
                icon = Icons.Default.Language,
                onClick = {
                    val newLang = if (currentLanguage == "en") "gu" else "en"
                    preferenceManager.saveLanguage(newLang)
                    currentLanguage = newLang
                    onRestartApp()
                }
            )

            SettingsItem(
                title = "Dark Mode",
                subtitle = if (isDarkMode) "On" else "Off",
                icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                trailing = {
                    Switch(checked = isDarkMode, onCheckedChange = {
                        isDarkMode = it
                        preferenceManager.setDarkMode(it)
                        onRestartApp()
                    })
                }
            )

            Text("Theme Color", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Color.White, Color(0xFFFFEBEE), Color(0xFFE3F2FD), Color(0xFFF1F8E9)).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable {
                                selectedColor = color.toArgb()
                                preferenceManager.saveBackgroundColor(selectedColor)
                                onRestartApp()
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}
