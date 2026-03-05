package com.example.gokudiyugam.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.FunctionEvent
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionsScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit,
    onNavigateToVideoPlayer: (String, String) -> Unit
) {
    var functionsList by remember { 
        mutableStateOf(preferenceManager.getFunctions().filter { !it.isExpired() }) 
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingEventWithoutUrl by remember { mutableStateOf<FunctionEvent?>(null) }
    
    // Form states
    var newFunctionName by remember { mutableStateOf("") }
    var newFunctionUrl by remember { mutableStateOf("") }
    var selectedExpiryTime by remember { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val currentUsername = preferenceManager.getCurrentUsername() ?: ""
    val canEdit = currentUserRole == UserRole.HOST || 
                 (currentUserRole == UserRole.SUB_HOST && preferenceManager.hasPermission(currentUsername, "screen_functions"))

    // Auto-cleanup on launch
    LaunchedEffect(Unit) {
        val current = preferenceManager.getFunctions()
        val valid = current.filter { !it.isExpired() }
        if (current.size != valid.size) {
            preferenceManager.saveFunctions(valid)
            functionsList = valid
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upcoming_functions), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            if (canEdit) {
                FloatingActionButton(
                    onClick = { 
                        newFunctionName = ""
                        newFunctionUrl = ""
                        selectedExpiryTime = null
                        showAddDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_function))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.functions_header_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }

            // Grid Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-20).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 8.dp)
            ) {
                if (functionsList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_events_scheduled),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(functionsList) { function ->
                            FunctionCard(
                                event = function,
                                isHost = canEdit,
                                onDelete = {
                                    val newList = functionsList.toMutableList()
                                    newList.remove(function)
                                    functionsList = newList
                                    preferenceManager.saveFunctions(newList)
                                },
                                onClick = { 
                                    if (function.url != null) {
                                        onNavigateToVideoPlayer(function.name, function.url)
                                    } else {
                                        viewingEventWithoutUrl = function
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            val initialTime = Calendar.getInstance()
            val timePickerState = rememberTimePickerState(
                initialHour = initialTime.get(Calendar.HOUR_OF_DAY),
                initialMinute = initialTime.get(Calendar.MINUTE),
                is24Hour = false
            )

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.add_new_function), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newFunctionName,
                            onValueChange = { newFunctionName = it },
                            label = { Text(stringResource(R.string.event_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newFunctionUrl,
                            onValueChange = { newFunctionUrl = it },
                            label = { Text(stringResource(R.string.video_url_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("https://www.youtube.com/watch?v=...") }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true }
                        ) {
                            OutlinedTextField(
                                value = if (selectedExpiryTime != null) {
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedExpiryTime!!))
                                } else stringResource(R.string.select_time),
                                onValueChange = { },
                                label = { Text(stringResource(R.string.auto_delete_time)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = Color.Transparent,
                                ),
                                trailingIcon = {
                                    Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.select_time))
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFunctionName.isNotBlank()) {
                                val newEvent = FunctionEvent(
                                    name = newFunctionName,
                                    url = if (newFunctionUrl.isBlank()) null else newFunctionUrl,
                                    expiryTime = selectedExpiryTime
                                )
                                
                                val newList = functionsList.toMutableList()
                                newList.add(newEvent)
                                functionsList = newList
                                preferenceManager.saveFunctions(newList)
                                
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                cal.set(Calendar.MINUTE, timePickerState.minute)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                
                                var expiry = cal.timeInMillis
                                if (expiry <= System.currentTimeMillis()) {
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                    expiry = cal.timeInMillis
                                }
                                selectedExpiryTime = expiry
                                showTimePicker = false
                            }
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    text = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TimePicker(state = timePickerState)
                        }
                    }
                )
            }
        }

        if (viewingEventWithoutUrl != null) {
            AlertDialog(
                onDismissRequest = { viewingEventWithoutUrl = null },
                title = { Text(viewingEventWithoutUrl!!.name, fontWeight = FontWeight.Bold) },
                text = { 
                    Text(stringResource(R.string.event_details_placeholder))
                },
                confirmButton = {
                    Button(onClick = { viewingEventWithoutUrl = null }) {
                        Text(stringResource(R.string.awesome))
                    }
                }
            )
        }
    }
}

@Composable
fun FunctionCard(
    event: FunctionEvent,
    isHost: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Icon(
                        if (event.url != null) Icons.Default.Link else Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                if (event.expiryTime != null) {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Text(
                        text = stringResource(R.string.ends_at, timeFormat.format(Date(event.expiryTime))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (isHost) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Functions - User")
@Composable
fun FunctionsScreenPreview() {
    GokudiyugamTheme {
        FunctionsScreen(
            preferenceManager = PreferenceManager(LocalContext.current),
            currentUserRole = UserRole.NORMAL,
            onBack = {},
            onNavigateToVideoPlayer = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Functions - Host")
@Composable
fun FunctionsScreenHostPreview() {
    GokudiyugamTheme {
        FunctionsScreen(
            preferenceManager = PreferenceManager(LocalContext.current),
            currentUserRole = UserRole.HOST,
            onBack = {},
            onNavigateToVideoPlayer = { _, _ -> }
        )
    }
}
