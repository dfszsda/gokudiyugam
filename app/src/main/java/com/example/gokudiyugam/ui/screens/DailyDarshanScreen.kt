package com.example.gokudiyugam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gokudiyugam.R
import com.example.gokudiyugam.ui.components.DarshanCard
import com.example.gokudiyugam.ui.theme.GokudiyugamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDarshanScreen(
    onBack: () -> Unit,
    onNavigateToPujaDarshan: () -> Unit,
    onNavigateToMandirDarshan: () -> Unit,
    onNavigateToGuruhariDarshan: () -> Unit,
    onNavigateToFestivals: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_darshan), fontWeight = FontWeight.Bold) },
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
                    text = stringResource(R.string.darshan_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }

            // Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-20).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 24.dp)
            ) {
                item { 
                    DarshanCard(
                        name = stringResource(R.string.mandir_darshan),
                        onClick = onNavigateToMandirDarshan
                    ) 
                }
                item { 
                    DarshanCard(
                        name = stringResource(R.string.puja_darshan),
                        onClick = onNavigateToPujaDarshan
                    ) 
                }
                item { 
                    DarshanCard(
                        name = stringResource(R.string.guruhari_darshan),
                        onClick = onNavigateToGuruhariDarshan
                    ) 
                }
                item { 
                    DarshanCard(
                        name = stringResource(R.string.festivals),
                        onClick = onNavigateToFestivals
                    ) 
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyDarshanScreenPreview() {
    GokudiyugamTheme {
        DailyDarshanScreen(onBack = {}, onNavigateToPujaDarshan = {}, onNavigateToMandirDarshan = {}, onNavigateToGuruhariDarshan = {}, onNavigateToFestivals = {})
    }
}
