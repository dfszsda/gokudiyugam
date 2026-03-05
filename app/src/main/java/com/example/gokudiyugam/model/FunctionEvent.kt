package com.example.gokudiyugam.model

import java.util.UUID

data class FunctionEvent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String? = null,
    val expiryTime: Long? = null
) {
    fun isExpired(): Boolean {
        return expiryTime != null && System.currentTimeMillis() > expiryTime
    }
}
