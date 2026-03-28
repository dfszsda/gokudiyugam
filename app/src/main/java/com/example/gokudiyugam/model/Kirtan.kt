package com.example.gokudiyugam.model

data class Kirtan(
    val id: String = "",
    val title: String = "",
    val artist: String = "BAPS",
    val resourceId: Int = 0,
    val category: String = "",
    val fileUri: String? = null,
    val lyrics: String? = null
)
