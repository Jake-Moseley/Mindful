package com.example.myapplication.data.model

class ChatMessage {
    enum class ChatRole {USER, ASSISTANT}

    data class ChatMessages(
        val role: ChatRole,
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isError: Boolean = false
    )
}