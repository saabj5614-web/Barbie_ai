package com.barbie.ai.model

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val fromUser: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
