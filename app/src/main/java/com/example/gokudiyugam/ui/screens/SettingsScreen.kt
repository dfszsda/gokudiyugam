package com.example.gokudiyugam.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.User
import com.example.gokudiyugam.network.GoogleSheetsUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    onBack: () -> Unit,
    onRestartApp: () -> Unit,
    onNavigateToHelpFeedback: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    var currentLanguage by remember { mutableStateOf(preferenceManager.getLanguage()) }
    var isDarkMode by remember { mutableStateOf(preferenceManager.isDarkMode()) }
    var selectedColor by remember { mutableIntStateOf(preferenceManager.getBackgroundColor()) }
    var defaultPlayer by remember { mutableStateOf(preferenceManager.getDefaultPlayer()) }
    
    val auth = FirebaseAuth.getInstance()
    val db = remember { FirebaseFirestore.getInstance("mediadata") }
    val currentUser = auth.currentUser
    
    // State for Profile Fields
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var profilePhotoUrl by remember { mutableStateOf("") }
    
    var isUpdating by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Password Reset Dialog States
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isRequestingPassword by remember { mutableStateOf(false) }

    // Color Picker Dialog
    var showColorPicker by remember { mutableStateOf(false) }
    var hexColorInput by remember { mutableStateOf(String.format("#%06X", (0xFFFFFF and selectedColor))) }

    // Player Selection Dialog
    var showPlayerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            db.collection("profile").document(uid).addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    firstName = user?.firstName ?: ""
                    middleName = user?.middleName ?: ""
                    lastName = user?.lastName ?: ""
                    mobileNumber = user?.mobileNumber ?: ""
                    dob = user?.dob ?: ""
                    gender = user?.gender ?: ""
                    email = user?.email?.ifEmpty { currentUser.email ?: "" } ?: currentUser.email ?: ""
                    profilePhotoUrl = user?.profilePhotoUrl ?: ""
                }
                isLoadingProfile = false
            }
        } else {
            isLoadingProfile = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profilePhotoUrl = it.toString() }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Date(millis)
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dob = formatter.format(date)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Request Password Reset") },
            text = {
                Column {
                    Text("Enter your registered email address to send a request to Admin.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotEmpty() && resetEmail.contains("@")) {
                            isRequestingPassword = true
                            db.collection("users")
                                .whereEqualTo("email", resetEmail)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (!result.isEmpty) {
                                        val userDoc = result.documents[0]
                                        val uid = userDoc.id
                                        val name = userDoc.getString("name") ?: "Unknown User"

                                        val requestData = mapOf(
                                            "uid" to uid,
                                            "email" to resetEmail,
                                            "name" to name,
                                            "status" to "pending",
                                            "timestamp" to System.currentTimeMillis()
                                        )

                                        db.collection("password_requests").document(uid).set(requestData)
                                            .addOnSuccessListener {
                                                isRequestingPassword = false
                                                showResetDialog = false
                                                Toast.makeText(context, "Request sent to Admin successfully!", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->
                                                isRequestingPassword = false
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        isRequestingPassword = false
                                        Toast.makeText(context, "This email is not registered!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isRequestingPassword
                ) {
                    if (isRequestingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Send Request")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Custom Background Color") },
            text = {
                Column {
                    Text("Enter Hex Color Code (e.g., #FFFFFF):", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hexColorInput,
                        onValueChange = { hexColorInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("#FFFFFF") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preset Colors:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(Color.White, Color(0xFFFFEBEE), Color(0xFFE3F2FD), Color(0xFFF1F8E9), Color(0xFFFFF3E0), Color(0xFFF3E5F5))
                        presets.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(35.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        hexColorInput = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val color = Color(android.graphics.Color.parseColor(hexColorInput))
                        selectedColor = color.toArgb()
                        preferenceManager.saveBackgroundColor(selectedColor)
                        showColorPicker = false
                        onRestartApp()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid Color Code", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPlayerDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerDialog = false },
            title = { Text("Select Default Video Player") },
            text = {
                Column {
                    listOf("ExoPlayer", "YouTube Player", "Auto (Hybrid)").forEach { player ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultPlayer = player
                                    preferenceManager.saveDefaultPlayer(player)
                                    showPlayerDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(selected = defaultPlayer == player, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(player)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlayerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
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
            Text("Profile Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (isLoadingProfile) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (!isEditing) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
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
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (firstName.isNotEmpty()) "$firstName $lastName" else "User Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (email.isNotEmpty()) Text(text = email, style = MaterialTheme.typography.bodyMedium)
                        if (mobileNumber.isNotEmpty()) Text(text = mobileNumber, style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile", fontSize = 12.sp)
                            }
                            
                            Button(
                                onClick = {
                                    val savedEmail = preferenceManager.getCurrentUsername()?.let { preferenceManager.getEmailForUser(it) }
                                    resetEmail = savedEmail ?: email
                                    showResetDialog = true 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset Password", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
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
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            readOnly = true,
                            enabled = false
                        )

                        ProfileTextField(value = mobileNumber, onValueChange = { mobileNumber = it }, label = "Mobile Number", icon = Icons.Default.Phone)
                        
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { },
                            label = { Text("Date of Birth") },
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("Gender:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                listOf("Male", "Female", "Other").forEach { option ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = option }.padding(end = 16.dp)) {
                                        RadioButton(selected = gender == option, onClick = { gender = option })
                                        Text(option, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    val uid = currentUser?.uid
                                    if (uid != null) {
                                        isUpdating = true
                                        val userProfile = mapOf(
                                            "firstName" to firstName,
                                            "middleName" to middleName,
                                            "lastName" to lastName,
                                            "email" to email,
                                            "mobileNumber" to mobileNumber,
                                            "dob" to dob,
                                            "gender" to gender,
                                            "profilePhotoUrl" to profilePhotoUrl,
                                            "uid" to uid
                                        )
                                        db.collection("profile").document(uid).set(userProfile, SetOptions.merge())
                                            .addOnSuccessListener {
                                                isUpdating = false
                                                isEditing = false
                                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                                
                                                lifecycleScope.launch {
                                                    val currentUname = preferenceManager.getCurrentUsername()
                                                    val role = currentUname?.let { preferenceManager.getUserRoleForAccount(it).name } ?: "NORMAL"
                                                    
                                                    // Point 2 Fix: Removed password parameter as per GoogleSheetsUploader update
                                                    GoogleSheetsUploader.uploadUserData(
                                                        firstName = firstName,
                                                        middleName = middleName,
                                                        lastName = lastName,
                                                        role = role,
                                                        gender = gender,
                                                        email = email,
                                                        mobileNumber = mobileNumber,
                                                        dob = dob,
                                                        uid = uid
                                                    )
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isUpdating = false
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isUpdating
                            ) {
                                if (isUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("App Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = stringResource(R.string.language),
                subtitle = when(currentLanguage) {
                    "gu" -> "ગુજરાતી"
                    "hi" -> "હિન્દી"
                    else -> "English"
                },
                icon = Icons.Default.Language,
                onClick = {
                    val nextLang = when(currentLanguage) {
                        "en" -> "gu"
                        "gu" -> "hi"
                        else -> "en"
                    }
                    preferenceManager.saveLanguage(nextLang)
                    currentLanguage = nextLang
                    onRestartApp()
                }
            )

            SettingsItem(
                title = stringResource(R.string.dark_mode),
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

            SettingsItem(
                title = stringResource(R.string.background_color),
                subtitle = hexColorInput,
                icon = Icons.Default.Palette,
                onClick = { showColorPicker = true },
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor))
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { showColorPicker = true }
                    )
                }
            )

            SettingsItem(
                title = "Video Player Type",
                subtitle = defaultPlayer,
                icon = Icons.Default.VideoLibrary,
                onClick = { showPlayerDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Other", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            SettingsItem(
                title = "About App",
                subtitle = "Version 1.9.0",
                icon = Icons.Default.Info,
                onClick = {
                    Toast.makeText(context, "Gokudiyugam - Spiritual Journey", Toast.LENGTH_SHORT).show()
                }
            )
            
            SettingsItem(
                title = "Help & Feedback",
                subtitle = "Contact support for any issues",
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = onNavigateToHelpFeedback
            )
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
