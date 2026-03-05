package com.example.gokudiyugam.model

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String = "",
    val name: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val gender: String = "",
    val dob: String = "",
    val profilePhotoUrl: String = "",
    val role: String = "user", // "host", "sub-host", "user"
    val permissions: List<String> = emptyList(),
    val isOnline: Boolean = false,
    val provider: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
