package com.example.gokudiyugam.model

data class Playlist(
    val id: String = "",
    val name: String = "",
    val userId: String = "",
    val kirtanIds: List<String> = emptyList()
)
