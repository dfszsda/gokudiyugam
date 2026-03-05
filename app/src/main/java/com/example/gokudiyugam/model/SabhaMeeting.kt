package com.example.gokudiyugam.model

import androidx.annotation.Keep

@Keep
data class SabhaTopic(
    val topicNameEn: String = "",
    val topicNameGu: String = "",
    val topicNameHi: String = "",
    val memberNameEn: String = "",
    val memberNameGu: String = "",
    val memberNameHi: String = ""
)

@Keep
data class SabhaMeeting(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mandalName: String = "",
    val sabhaName: String = "",
    val dateTime: String = "",
    val topics: List<SabhaTopic> = emptyList()
)
