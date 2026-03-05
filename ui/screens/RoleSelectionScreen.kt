package com.example.gokudiyugam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gokudiyugam.model.UserRole

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select Your Role", fontSize = 24.sp, modifier = Modifier.padding(bottom = 32.dp))

        Button(
            onClick = { onRoleSelected(UserRole.HOST) },
            modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp)
        ) {
            Text("Host User (Login Required)")
        }

        Button(
            onClick = { onRoleSelected(UserRole.NORMAL) },
            modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp)
        ) {
            Text("Normal User")
        }

        Button(
            onClick = { onRoleSelected(UserRole.GUEST) },
            modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp)
        ) {
            Text("Guest User")
        }
    }
}