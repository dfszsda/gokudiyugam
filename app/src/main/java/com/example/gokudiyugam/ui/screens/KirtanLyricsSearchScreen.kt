package com.example.gokudiyugam.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gokudiyugam.model.Kirtan
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KirtanLyricsSearchScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var allKirtans by remember { mutableStateOf<List<Kirtan>>(emptyList()) }
    var filteredKirtans by remember { mutableStateOf<List<Kirtan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedKirtan by remember { mutableStateOf<Kirtan?>(null) }

    val db = FirebaseFirestore.getInstance("mediadata")

    LaunchedEffect(Unit) {
        db.collection("mediadata")
            .whereEqualTo("mediaType", "audio")
            .get()
            .addOnSuccessListener { snapshot ->
                allKirtans = snapshot.documents.mapNotNull { doc ->
                    val lyrics = doc.getString("lyrics")
                    if (!lyrics.isNullOrBlank()) {
                        Kirtan(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            lyrics = lyrics,
                            fileUri = doc.getString("url")
                        )
                    } else null
                }
                filteredKirtans = allKirtans
                isLoading = false
            }
    }

    // Search Logic with basic English to Gujarati mapping support
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            filteredKirtans = allKirtans
        } else {
            val query = searchQuery.lowercase()
            filteredKirtans = allKirtans.filter { kirtan ->
                kirtan.title.lowercase().contains(query) || 
                kirtan.lyrics?.lowercase()?.contains(query) == true ||
                containsTransliterated(kirtan.title.lowercase(), query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kirtan Lyrics Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (selectedKirtan != null) selectedKirtan = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedKirtan == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search Lyrics (English/Gujarati)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        items(filteredKirtans) { kirtan ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedKirtan = kirtan },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = kirtan.title, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            } else {
                // Display Lyrics
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(text = selectedKirtan!!.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Surface(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                text = selectedKirtan!!.lyrics ?: "No lyrics available.",
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Very basic transliteration check helper
fun containsTransliterated(text: String, query: String): Boolean {
    // English to Gujarati basic phonetics map
    val mapping = mapOf(
        "a" to "અ", "k" to "ક", "kh" to "ખ", "g" to "ગ", "gh" to "ઘ",
        "ch" to "ચ", "chh" to "છ", "j" to "જ", "jh" to "ઝ",
        "t" to "ત", "th" to "થ", "d" to "દ", "dh" to "ધ", "n" to "ન",
        "p" to "પ", "f" to "ફ", "b" to "બ", "bh" to "ભ", "m" to "મ",
        "y" to "ય", "r" to "ર", "l" to "લ", "v" to "વ", "s" to "સ", "h" to "હ",
        "sh" to "શ"
    )
    
    // Check if query is English but text is Gujarati
    // If any mapped Gujarati character is found in text, return true
    query.forEach { char ->
        val guj = mapping[char.toString()]
        if (guj != null && text.contains(guj)) return true
    }
    
    return false
}
