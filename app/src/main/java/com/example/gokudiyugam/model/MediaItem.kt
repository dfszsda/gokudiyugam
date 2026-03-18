package com.example.gokudiyugam.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

@Keep
data class MediaItem(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val type: String = "", // "photo", "video", "audio", "doc", "sabha_timetable"
    val mediaType: String = "", // "photo", "video", "audio", "doc", "file"
    val uploadedBy: String = "",
    val canDownload: Boolean = true, // New field for download permission
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
