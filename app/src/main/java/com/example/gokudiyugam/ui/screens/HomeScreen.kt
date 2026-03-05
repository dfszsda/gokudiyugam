package com.example.gokudiyugam.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUserRole: UserRole? = null,
    onNavigateToDailyDarshan: () -> Unit,
    onNavigateToKirtan: () -> Unit,
    onNavigateToSabhaTimeTable: () -> Unit,
    onNavigateToFunctions: () -> Unit,
    onNavigateToSatsangNews: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdminPanel: () -> Unit = {},
    onNavigateToMediaLibrary: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToGoogleDrive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(
                            stringResource(R.string.baps_mandal),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            stringResource(R.string.badalpur),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { 
                            if (currentUserRole != null) showMenu = true else onProfileClick()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(28.dp)
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
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Hero Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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
                                text = stringResource(R.string.welcome_spiritual_home),
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quick_services),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { /* TODO */ }) {
                    Text(stringResource(R.string.see_all), color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Admin Panel accessible to HOST and SUB_HOST
                if (currentUserRole == UserRole.HOST || currentUserRole == UserRole.SUB_HOST) {
                    item {
                        FeatureCard(
                            title = "Admin Panel",
                            icon = Icons.Default.AdminPanelSettings,
                            description = "Manage users & media",
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
                        icon = Icons.Default.Schedule,
                        description = stringResource(R.string.weekly_schedule),
                        onClick = onNavigateToSabhaTimeTable
                    ) 
                }
                item { 
                    FeatureCard(
                        title = stringResource(R.string.functions),
                        icon = Icons.Default.Event,
                        description = stringResource(R.string.upcoming_events),
                        onClick = onNavigateToFunctions
                    ) 
                }
                item { 
                    FeatureCard(
                        title = stringResource(R.string.satsang_news),
                        icon = Icons.Default.Newspaper,
                        description = stringResource(R.string.latest_updates),
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
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        currentUserRole = UserRole.HOST,
        onNavigateToDailyDarshan = {},
        onNavigateToKirtan = {},
        onNavigateToSabhaTimeTable = {},
        onNavigateToFunctions = {},
        onNavigateToSatsangNews = {},
        onNavigateToSettings = {},
        onNavigateToMediaLibrary = {},
        onProfileClick = {},
        onLogout = {},
        onNavigateToGoogleDrive = {}
    )
}
