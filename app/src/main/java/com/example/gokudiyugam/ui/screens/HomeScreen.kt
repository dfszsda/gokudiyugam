package com.example.gokudiyugam.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentUserRole: UserRole? = null,
    onNavigateToDailyDarshan: () -> Unit,
    onNavigateToKirtan: () -> Unit,
    onNavigateToSabhaTimeTable: () -> Unit,
    onNavigateToFunctions: () -> Unit,
    onNavigateToSatsangNews: () -> Unit,
    onNavigateToSabhaSaar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMediaLibrary: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToGoogleDrive: () -> Unit,
    onNavigateToAdminPanel: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance("mediadata")
    val homeImages = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        db.collection("home_images").addSnapshotListener { snapshot, _ ->
            homeImages.clear()
            snapshot?.documents?.forEach { doc ->
                doc.getString("url")?.let { homeImages.add(it) }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // Using only bottom padding
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Image Slider Header (Integrated with Top Bar functionality)
            Box(modifier = Modifier.fillMaxWidth()) {
                if (homeImages.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { homeImages.size })
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(5000)
                            if (homeImages.size > 1) {
                                val nextPage = (pagerState.currentPage + 1) % homeImages.size
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f) // 16:9 aspect ratio
                    ) { page ->
                        AsyncImage(
                            model = homeImages[page],
                            contentDescription = "Slider Image $page",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Placeholder if no images
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                // Top Bar Overlay (Gradient for visibility)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                                endY = 200f
                            )
                        )
                )

                // Navigation & Profile Controls Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.icon),
                                contentDescription = "App Logo",
                                modifier = Modifier.padding(4.dp),
                                contentScale = ContentScale.Inside
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.baps_mandal),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { if (currentUserRole != null) showMenu = true else onProfileClick() },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.logout)) },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Welcome Card Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.photo),
                        contentDescription = "Welcome Image",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    startY = 100f
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.jay_swaminarayan),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "BAPS Shri Swaminarayan Mandir, Badalpur",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Service Header
            Text(
                text = stringResource(R.string.quick_services),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grid for Services
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .heightIn(max = 2000.dp)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                userScrollEnabled = false 
            ) {
                if (currentUserRole == UserRole.HOST || currentUserRole == UserRole.SUB_HOST) {
                    item {
                        FeatureCard(
                            title = "Admin Panel",
                            icon = Icons.Default.AdminPanelSettings,
                            description = "Manage Content",
                            onClick = onNavigateToAdminPanel
                        )
                    }
                }
                item { 
                    FeatureCard(
                        title = stringResource(R.string.daily_darshan),
                        icon = Icons.Default.Image,
                        description = stringResource(R.string.morning_darshan),
                        onClick = onNavigateToDailyDarshan
                    )
                }
                item { 
                    FeatureCard(
                        title = stringResource(R.string.kirtan),
                        icon = Icons.Default.MusicNote,
                        description = stringResource(R.string.devotional_songs),
                        onClick = onNavigateToKirtan
                    )
                }
                item { 
                    FeatureCard(
                        title = stringResource(R.string.sabha),
                        icon = Icons.Default.Event,
                        description = stringResource(R.string.weekly_schedule),
                        onClick = onNavigateToSabhaTimeTable
                    )
                }
                item { 
                    FeatureCard(
                        title = stringResource(R.string.functions),
                        icon = Icons.Default.Celebration,
                        description = stringResource(R.string.upcoming_events),
                        onClick = onNavigateToFunctions
                    )
                }
                item { 
                    FeatureCard(
                        title = "Sabha Saar",
                        icon = Icons.Default.Newspaper,
                        description = stringResource(R.string.latest_updates),
                        onClick = onNavigateToSabhaSaar
                    )
                }
                item { 
                    FeatureCard(
                        title = "Satsang News",
                        icon = Icons.Default.Campaign,
                        description = "Coming Soon...",
                        onClick = onNavigateToSatsangNews
                    )
                }
                item { 
                    FeatureCard(
                        title = stringResource(R.string.settings),
                        icon = Icons.Default.Settings,
                        description = stringResource(R.string.preferences),
                        onClick = onNavigateToSettings
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
