package com.example.notiapp

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val sessionId: Long,
    val status: String, // "INITIALIZING", "READY", "ERROR"
    val audioFileId: Long
)

data class SessionResponse(
    val sessionId: Long,
    val status: String
)