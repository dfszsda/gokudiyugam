package com.example.gokudiyugam.ui.screens

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.R
import com.example.gokudiyugam.data.KirtanRepository
import com.example.gokudiyugam.service.KirtanAudioService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KirtanPlayerScreen(
    category: String,
    onBack: () -> Unit,
    kirtanViewModel: KirtanViewModel = viewModel()
) {
    val context = LocalContext.current
    val kirtans = when (category) {
        stringResource(R.string.kirtan_category_arati) -> KirtanRepository.getAratiKirtans(context)
        stringResource(R.string.kirtan_category_dhun) -> KirtanRepository.getDhunKirtans(context)
        stringResource(R.string.kirtan_category_prathana) -> KirtanRepository.getPrathanaKirtans(context)
        stringResource(R.string.kirtan_category_others) -> KirtanRepository.getOthersKirtans(context)
        stringResource(R.string.kirtan_category_favorite) -> KirtanRepository.getFavoriteKirtans(context)
        "Shared Kirtans" -> kirtanViewModel.sharedKirtans
        else -> emptyList()
    }

    LaunchedEffect(category) {
        kirtanViewModel.initController(context)
        context.startService(Intent(context, KirtanAudioService::class.java))
        
        if (category == "Shared Kirtans") {
            kirtanViewModel.fetchSharedKirtans()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category, fontWeight = FontWeight.Bold) },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = kirtanViewModel.currentKirtan?.title ?: stringResource(R.string.now_playing),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Seek bar
            Slider(
                value = kirtanViewModel.playbackPosition.toFloat(),
                onValueChange = { /* TODO: Implement seek */ },
                valueRange = 0f..(kirtanViewModel.duration.toFloat().takeIf { it > 0 } ?: 1f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { kirtanViewModel.playPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = { kirtanViewModel.skipBackward() }) {
                    Icon(Icons.Default.Replay5, contentDescription = "Backward 5s")
                }
                IconButton(
                    onClick = { kirtanViewModel.togglePlayPause() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (kirtanViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (kirtanViewModel.isPlaying) stringResource(R.string.paused) else stringResource(R.string.playing),
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = { kirtanViewModel.skipForward() }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                }
                IconButton(onClick = { kirtanViewModel.playNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Kirtan list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(kirtans) { kirtan ->
                    val isFavorite = kirtanViewModel.isFavorite(kirtan)
                    
                    Surface(
                        onClick = { kirtanViewModel.playKirtan(context, kirtan, kirtans) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (kirtanViewModel.currentKirtan?.id == kirtan.id) 
                            MaterialTheme.colorScheme.secondaryContainer 
                        else MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = kirtan.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (kirtanViewModel.currentKirtan?.id == kirtan.id) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = { kirtanViewModel.toggleFavorite(context, kirtan) }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
