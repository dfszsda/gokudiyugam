@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.ai.AIService
import com.example.gokudiyugam.model.SabhaMeeting
import com.example.gokudiyugam.model.SabhaTopic
import com.example.gokudiyugam.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabhaDetailScreen(
    currentUserRole: UserRole?,
    sabhaName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = rememberCoroutineScope()
    
    val currentUsername = preferenceManager.getCurrentUsername() ?: ""
    val isHost = currentUserRole == UserRole.HOST
    val canEdit = isHost || 
                 (currentUserRole == UserRole.SUB_HOST && preferenceManager.hasPermission(currentUsername, "screen_sabha_timetable"))

    // State for data
    val mandals = remember { mutableStateListOf<String>().apply { addAll(preferenceManager.getMandals()) } }
    val sabhaList = listOf(
        "Yuva Sabha", "Yuvati Sabha", "Bal Sabha", "Balika Sabha", 
        "Shishu Sabha", "Samyukta Sabha", "Mahila Sabha", "BSS Sabha", "YST Sabha"
    )
    val languages = listOf("English", "Gujarati", "Hindi")

    var selectedMandal by remember { mutableStateOf("Badalpur") }
    var selectedSabha by remember { mutableStateOf(if (sabhaList.contains(sabhaName)) sabhaName else sabhaList.first()) }
    var selectedLanguage by remember { mutableStateOf("English") }
    
    var inputTopicNameEn by remember { mutableStateOf("") }
    var inputMemberNameEn by remember { mutableStateOf("") }
    
    // Translation states
    var inputTopicNameGu by remember { mutableStateOf("") }
    var inputTopicNameHi by remember { mutableStateOf("") }
    var inputMemberNameGu by remember { mutableStateOf("") }
    var inputMemberNameHi by remember { mutableStateOf("") }
    
    var showTranslationFields by remember { mutableStateOf(false) }
    
    // AI Processing States
    var isAiProcessing by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    
    val meetingTopics = remember { mutableStateListOf<SabhaTopic>() }

    var showAddMandalDialog by remember { mutableStateOf(false) }
    var newMandalName by remember { mutableStateOf("") }

    // Load existing meeting
    LaunchedEffect(selectedMandal, selectedSabha) {
        val existingMeetings = preferenceManager.getSabhaMeetings()
        val meeting = existingMeetings.find { it.mandalName == selectedMandal && it.sabhaName == selectedSabha }
        meetingTopics.clear()
        meeting?.topics?.let { meetingTopics.addAll(it) }
    }

    val backgroundColor = Color(0xFFFFCC80)
    val cardBackgroundColor = Color.White
    val innerBoxColor = Color(0xFFFDF2E9)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sabha_schedule), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (isHost && meetingTopics.isNotEmpty()) {
                        IconButton(onClick = {
                            meetingTopics.clear()
                            val meetings = preferenceManager.getSabhaMeetings().toMutableList()
                            meetings.removeAll { it.mandalName == selectedMandal && it.sabhaName == selectedSabha }
                            preferenceManager.saveSabhaMeetings(meetings)
                            Toast.makeText(context, "Sabha timetable cleared", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectionRow(
                isHost = canEdit,
                mandal = selectedMandal,
                sabha = selectedSabha,
                language = selectedLanguage,
                mandals = mandals,
                sabhas = sabhaList,
                languages = languages,
                onMandalChange = { selectedMandal = it },
                onSabhaChange = { selectedSabha = it },
                onLanguageChange = { selectedLanguage = it },
                onAddMandal = { showAddMandalDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = cardBackgroundColor,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (canEdit) {
                        Text(
                            text = stringResource(R.string.add_topic), 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = inputTopicNameEn,
                            onValueChange = { inputTopicNameEn = it; aiErrorMessage = null },
                            label = { Text(stringResource(R.string.topic_name) + " (English)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = aiErrorMessage != null
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = inputMemberNameEn,
                            onValueChange = { inputMemberNameEn = it; aiErrorMessage = null },
                            label = { Text(stringResource(R.string.enter_name) + " (English)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = aiErrorMessage != null
                        )

                        if (showTranslationFields) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Translations (Verify before adding):", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputTopicNameGu,
                                onValueChange = { inputTopicNameGu = it },
                                label = { Text("Topic (ગુજરાતી)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputMemberNameGu,
                                onValueChange = { inputMemberNameGu = it },
                                label = { Text("Name (ગુજરાતી)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputTopicNameHi,
                                onValueChange = { inputTopicNameHi = it },
                                label = { Text("Topic (हिन्दी)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputMemberNameHi,
                                onValueChange = { inputMemberNameHi = it },
                                label = { Text("Name (हिन्दी)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (aiErrorMessage != null) {
                            Text(
                                text = aiErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isAiProcessing) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text("Translating...", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!showTranslationFields) {
                                    Button(
                                        onClick = {
                                            if (inputTopicNameEn.isNotBlank() && inputMemberNameEn.isNotBlank()) {
                                                scope.launch {
                                                    isAiProcessing = true
                                                    
                                                    // Use Lipi (Mapping) for transliteration as requested
                                                    inputTopicNameGu = AIService.translateByMapping(inputTopicNameEn, "gu")
                                                    inputTopicNameHi = AIService.translateByMapping(inputTopicNameEn, "hi")
                                                    inputMemberNameGu = AIService.translateByMapping(inputMemberNameEn, "gu")
                                                    inputMemberNameHi = AIService.translateByMapping(inputMemberNameEn, "hi")
                                                    
                                                    showTranslationFields = true
                                                    isAiProcessing = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Spellcheck, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Translate (Lipi)")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (inputTopicNameEn.isNotBlank() && inputMemberNameEn.isNotBlank()) {
                                                val newTopic = SabhaTopic(
                                                    topicNameEn = inputTopicNameEn,
                                                    topicNameGu = inputTopicNameGu.ifBlank { inputTopicNameEn },
                                                    topicNameHi = inputTopicNameHi.ifBlank { inputTopicNameEn },
                                                    memberNameEn = inputMemberNameEn,
                                                    memberNameGu = inputMemberNameGu.ifBlank { inputMemberNameEn },
                                                    memberNameHi = inputMemberNameHi.ifBlank { inputMemberNameEn }
                                                )
                                                
                                                meetingTopics.add(newTopic)
                                                
                                                // Reset fields
                                                inputTopicNameEn = ""; inputMemberNameEn = ""
                                                inputTopicNameGu = ""; inputMemberNameGu = ""
                                                inputTopicNameHi = ""; inputMemberNameHi = ""
                                                showTranslationFields = false
                                                
                                                Toast.makeText(context, "Topic Added to List", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Topic")
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { showTranslationFields = false },
                                        modifier = Modifier.weight(0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    Text(
                        text = stringResource(R.string.meeting_details), 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    MeetingTopicsList(
                        sabha = selectedSabha, 
                        mandal = selectedMandal, 
                        language = selectedLanguage, 
                        topics = meetingTopics, 
                        bgColor = innerBoxColor,
                        isHost = isHost,
                        onDeleteTopic = { topic ->
                            meetingTopics.remove(topic)
                        }
                    )

                    if (canEdit && meetingTopics.isNotEmpty()) {
                        Button(
                            onClick = {
                                val meetings = preferenceManager.getSabhaMeetings().toMutableList()
                                meetings.removeAll { it.mandalName == selectedMandal && it.sabhaName == selectedSabha }
                                meetings.add(SabhaMeeting(
                                    mandalName = selectedMandal,
                                    sabhaName = selectedSabha,
                                    dateTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                                    topics = meetingTopics.toList()
                                ))
                                preferenceManager.saveSabhaMeetings(meetings)
                                Toast.makeText(context, "Meeting Saved Successfully", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.save_meeting))
                        }
                    }
                }
            }
        }
    }

    if (showAddMandalDialog) {
        AddMandalDialog(newMandalName, { newMandalName = it }, {
            if (newMandalName.isNotBlank()) {
                mandals.add(newMandalName)
                preferenceManager.saveMandals(mandals.toList())
                selectedMandal = newMandalName
                newMandalName = ""
                showAddMandalDialog = false
            }
        }, { showAddMandalDialog = false })
    }
}

@Composable
fun MeetingTopicsList(
    sabha: String, 
    mandal: String, 
    language: String, 
    topics: List<SabhaTopic>, 
    bgColor: Color,
    isHost: Boolean,
    onDeleteTopic: (SabhaTopic) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp), 
        shape = RoundedCornerShape(16.dp), 
        color = bgColor,
        border = BorderStroke(1.dp, Color(0xFFFFCC80).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$sabha - $mandal ($language)", 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFFE67E22),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (topics.isEmpty()) {
                Text(
                    "No topics scheduled for this week.", 
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), 
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                topics.forEachIndexed { i, t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val topicName = when (language) {
                            "Gujarati" -> t.topicNameGu
                            "Hindi" -> t.topicNameHi
                            else -> t.topicNameEn
                        }
                        val memberName = when (language) {
                            "Gujarati" -> t.memberNameGu
                            "Hindi" -> t.memberNameHi
                            else -> t.memberNameEn
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${i+1}. $topicName",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = memberName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        if (isHost) {
                            IconButton(onClick = { onDeleteTopic(t) }) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete", 
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    if (i < topics.size - 1) HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun SelectionRow(
    isHost: Boolean,
    mandal: String,
    sabha: String,
    language: String,
    mandals: List<String>,
    sabhas: List<String>,
    languages: List<String>,
    onMandalChange: (String) -> Unit,
    onSabhaChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onAddMandal: () -> Unit
) {
    var mExp by remember { mutableStateOf(false) }
    var sExp by remember { mutableStateOf(false) }
    var lExp by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { mExp = true }, 
                    shape = RoundedCornerShape(16.dp), 
                    color = Color.White, 
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(mandal, modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = mExp, onDismissRequest = { mExp = false }) {
                    mandals.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { onMandalChange(m); mExp = false }) }
                    if (isHost) DropdownMenuItem(text = { Text("Add Mandal") }, onClick = { onAddMandal(); mExp = false })
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { if (isHost) sExp = true }, 
                    shape = RoundedCornerShape(16.dp), 
                    color = Color.White, 
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sabha, modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                        if (isHost) Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = sExp, onDismissRequest = { sExp = false }) {
                    sabhas.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { onSabhaChange(s); sExp = false }) }
                }
            }
        }
        
        // Language Selection Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { lExp = true }, 
                shape = RoundedCornerShape(16.dp), 
                color = Color.White, 
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(language, modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
            DropdownMenu(expanded = lExp, onDismissRequest = { lExp = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                languages.forEach { l -> DropdownMenuItem(text = { Text(l) }, onClick = { onLanguageChange(l); lExp = false }) }
            }
        }
    }
}

@Composable
fun AddMandalDialog(name: String, onNameChange: (String) -> Unit, onAdd: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Add New Mandal") }, 
        text = { 
            OutlinedTextField(
                value = name, 
                onValueChange = onNameChange, 
                label = { Text("Mandal Name") },
                shape = RoundedCornerShape(12.dp)
            ) 
        },
        confirmButton = { Button(onClick = onAdd) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
