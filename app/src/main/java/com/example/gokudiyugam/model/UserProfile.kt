package com.example.gokudiyugam.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "NORMAL",
    val permissions: List<String> = emptyList(),
    val canDelete: Boolean = false
)