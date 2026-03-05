@file:Suppress("MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT")

package com.example.gokudiyugam.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.drive.DriveViewModel
import com.example.gokudiyugam.model.MediaItem
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import kotlinx.coroutines.delay

enum class FestivalView {
    MAIN, PHOTOS, VIDEOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivalsScreen(
    preferenceManager: PreferenceManager,
    currentUserRole: UserRole?,
    onBack: () -> Unit,
    onNavigateToVideoPlayer: (String, String) -> Unit,
    driveViewModel: DriveViewModel = viewModel()
) {
    var currentView by remember { mutableStateOf(FestivalView.MAIN) }
    val currentUsername = preferenceManager.getCurrentUsername() ?: ""
    val canEdit = currentUserRole == UserRole.HOST || 
                 (currentUserRole == UserRole.SUB_HOST && preferenceManager.hasPermission(currentUsername, "screen_festivals"))

    LaunchedEffect(currentView) {
        if (currentView == FestivalView.PHOTOS) {
            driveViewModel.fetchCategoryItems("photo")
        } else if (currentView == FestivalView.VIDEOS) {
            driveViewModel.fetchCategoryItems("video")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = when (currentView) {
                        FestivalView.MAIN -> R.string.festivals
                        FestivalView.PHOTOS -> R.string.photo
                        FestivalView.VIDEOS -> R.string.video
                    }
                    Text(stringResource(titleRes), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentView == FestivalView.MAIN) {
                            onBack()
                        } else {
                            currentView = FestivalView.MAIN
                        }
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FestivalsContent(
                driveViewModel = driveViewModel,
                canEdit = canEdit,
                currentView = currentView,
                onViewChange = { currentView = it },
                onNavigateToVideoPlayer = onNavigateToVideoPlayer
            )
        }
    }
}

@Composable
fun FestivalsContent(
    driveViewModel: DriveViewModel,
    canEdit: Boolean,
    currentView: FestivalView,
    onViewChange: (FestivalView) -> Unit,
    onNavigateToVideoPlayer: (String, String) -> Unit
) {
    when (currentView) {
        FestivalView.MAIN -> MainFestivalsSelection(onViewChange)
        FestivalView.PHOTOS -> PhotosGalleryPage(driveViewModel, canEdit)
        FestivalView.VIDEOS -> VideosGalleryPage(driveViewModel, canEdit, onNavigateToVideoPlayer)
    }
}

@Composable
fun PhotosGalleryPage(driveViewModel: DriveViewModel, canEdit: Boolean) {
    val photos = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching
    var selectedPhoto by remember { mutableStateOf<MediaItem?>(null) }

    if (isFetching && photos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(photos) { photoItem ->
                Image(
                    painter = rememberAsyncImagePainter(photoItem.url),
                    contentDescription = photoItem.title,
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { selectedPhoto = photoItem },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    if (selectedPhoto != null) {
        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            confirmButton = { TextButton(onClick = { selectedPhoto = null }) { Text("Close") } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedPhoto?.url),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(text = selectedPhoto?.title ?: "", modifier = Modifier.padding(top = 8.dp))
                }
            }
        )
    }
}

@Composable
fun VideosGalleryPage(
    driveViewModel: DriveViewModel,
    canEdit: Boolean,
    onNavigateToVideoPlayer: (String, String) -> Unit
) {
    val videos = driveViewModel.currentCategoryItems
    val isFetching = driveViewModel.isFetching

    if (isFetching && videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(videos) { videoItem ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onNavigateToVideoPlayer(videoItem.title, videoItem.url) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(videoItem.title, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MainFestivalsSelection(onViewChange: (FestivalView) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FestivalCategoryCard(title = "Photos", onClick = { onViewChange(FestivalView.PHOTOS) })
        FestivalCategoryCard(title = "Videos", onClick = { onViewChange(FestivalView.VIDEOS) })
    }
}

@Composable
fun FestivalCategoryCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
