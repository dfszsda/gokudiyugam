package com.example.gokudiyugam.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

@Keep
data class MediaItem(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val type: String = "", // "photo", "video", "audio", "doc"
    val uploadedBy: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
