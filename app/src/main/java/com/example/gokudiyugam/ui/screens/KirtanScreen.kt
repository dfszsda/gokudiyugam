package com.example.gokudiyugam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.R
import com.example.gokudiyugam.model.UserRole
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KirtanScreen(
    onBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    kirtanViewModel: KirtanViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val preferenceManager = remember { PreferenceManager(context) }
    
    val currentUser = preferenceManager.getCurrentUsername() ?: ""
    val userRole = preferenceManager.getUserRoleForAccount(currentUser)
    val permissions = preferenceManager.getUserPermissions(currentUser)
    
    // Logic: Only HOST or SUB_HOST with 'screen_kirtan' permission can see Shared Kirtans
    val canSeeShared = userRole == UserRole.HOST || (userRole == UserRole.SUB_HOST && permissions.contains("screen_kirtan"))

    var sharedKirtanCount by remember { mutableIntStateOf(0) }

    val baseCategories = listOf(
        stringResource(R.string.kirtan_category_arati),
        stringResource(R.string.kirtan_category_thal),
        stringResource(R.string.kirtan_category_dhun),
        stringResource(R.string.kirtan_category_prathana),
        stringResource(R.string.kirtan_category_bhajan),
        stringResource(R.string.kirtan_category_puja_vidhi),
        stringResource(R.string.kirtan_category_others),
        stringResource(R.string.kirtan_category_favorite)
    )

    // Build the final list based on permissions
    val kirtanCategories = remember(canSeeShared) {
        if (canSeeShared) baseCategories + "Shared Kirtans" else baseCategories
    }

    // Listen to shared kirtans count from Firestore
    LaunchedEffect(Unit) {
        kirtanViewModel.initController(context)
        if (canSeeShared) {
            db.collection("shared_data")
                .whereEqualTo("category", "Kirtan")
                .addSnapshotListener { snapshot, _ ->
                    sharedKirtanCount = snapshot?.size() ?: 0
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kirtan_muktavali), fontWeight = FontWeight.Bold) },
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
                    text = stringResource(R.string.kirtan_header_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }

            // Categories Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-20).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 24.dp)
            ) {
                items(kirtanCategories) { category ->
                    val isFavoriteCategory = category == stringResource(R.string.kirtan_category_favorite)
                    val isSharedCategory = category == "Shared Kirtans"
                    
                    KirtanCategoryCard(
                        label = category,
                        isFavorite = isFavoriteCategory,
                        isShared = isSharedCategory,
                        count = if (isFavoriteCategory) kirtanViewModel.favoriteKirtans.size else if (isSharedCategory) sharedKirtanCount else 0,
                        onClick = { onNavigateToPlayer(category) }
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                stringResource(R.string.coming_soon),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KirtanCategoryCard(
    label: String, 
    isFavorite: Boolean = false,
    isShared: Boolean = false,
    count: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFavorite -> MaterialTheme.colorScheme.secondaryContainer
                isShared -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (count > 0) {
                        Badge { Text(count.toString()) }
                    }
                }
            ) {
                Icon(
                    imageVector = when {
                        isFavorite -> Icons.Default.Favorite
                        isShared -> Icons.Default.CloudDownload
                        else -> Icons.Default.MusicNote
                    },
                    contentDescription = null,
                    tint = when {
                        isFavorite -> Color.Red
                        isShared -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = when {
                    isFavorite -> MaterialTheme.colorScheme.onSecondaryContainer
                    isShared -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KirtanPagePreview() {
    GokudiyugamTheme {
        KirtanScreen(onBack = {}, onNavigateToPlayer = {})
    }
}
